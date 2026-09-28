import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.CRC32;

/** Synthetic owned files and child JVMs only; never starts Minecraft or touches an Academy. */
public final class SupervisorTest {
    private static int checks, processes;
    private static final Host.Supervision FAST = new Host.Supervision(5000, 1000, 1500, 1000, 50);
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    @FunctionalInterface interface Checked { void run() throws Exception; }
    private static IOException rejected(Checked operation, String message) throws Exception {
        try { operation.run(); } catch (IOException expected) { checks++; return expected; }
        throw new AssertionError(message);
    }
    private static String status(long epoch, String state) {
        return "{\"state\":\"" + state + "\",\"epoch_millis\":" + epoch + "}";
    }

    private static void health() {
        long start = 1_000_000;
        Host.Supervision t = new Host.Supervision(3000, 1000, 2000, 1000, 10);
        Host.SupervisionHealth h = new Host.SupervisionHealth(start, t);
        check(h.observe(null, 2999, start + 2999) == null, "startup grace");
        check(h.observe(null, 3000, start + 3000).contains("startup"), "missing startup expires");
        for (String invalid : List.of(status(start - 1, "failed"), status(start + 10000, "running"),
                "{}", "{\"state\":\"running\",\"epoch_millis\":1.5}",
                "{\"state\":\"running\",\"epoch_millis\":9223372036854775808}",
                "{\"state\":\"running\",\"epoch_millis\":1000000,\"epoch_millis\":1000001}",
                "{\"state\":\"running\",\"state\":\"failed\",\"epoch_millis\":1000000}",
                status(start, "unrecognized"))) {
            h = new Host.SupervisionHealth(start, t);
            check(h.observe(invalid, 0, start) == null && !h.seen, "invalid startup does not acquire health");
            check(h.observe(invalid, 3000, start + 3000).contains("startup"), "invalid startup expires");
        }
        h = new Host.SupervisionHealth(start, t);
        check(h.observe(status(start, "running"), 0, start) == null && h.seen, "first live report");
        check(h.observe(status(start, "running"), 999, start + 999) == null, "same report grace");
        check(h.observe(status(start, "running"), 1000, start + 1000).contains("stopped advancing"), "same report cannot renew health");
        h = new Host.SupervisionHealth(start, t);
        for (int i = 0; i < 100; i++)
            check(h.observe(status(start + i * 500, i % 2 == 0 ? "running" : "paused"), i * 500, start + i * 500) == null,
                    "paused and running are live when reports advance");
        check(h.observe(status(start + 49000, "running"), 50499, start + 50499) == null, "older report cannot renew");
        check(h.observe(null, 50500, start + 50500).contains("stopped advancing"), "removed report expires");
        h = new Host.SupervisionHealth(start, t);
        check(h.observe(status(start, "failed"), 0, start).contains("reported a failure"), "immediate failed report");
        h = new Host.SupervisionHealth(start, t);
        h.observe(status(start, "running"), 0, start);
        check(h.observe(status(start, "running"), 1000, start - 100).contains("stopped advancing"), "monotonic deadline despite clock rewind");
        check(Host.SUPERVISION.startupMillis() == 180000 && Host.SUPERVISION.staleMillis() == 60000
                && Host.SUPERVISION.stopMillis() == 35000, "production deadlines not shortened by tests");
    }

    private static void logs(Path root) throws Exception {
        Random random = new Random(88271);
        for (int limit : new int[] {1, 7, 64}) for (int length : new int[] {0, 1, 7, 64, 65, 513}) {
            Path dir = Files.createDirectory(root.resolve("log-" + limit + "-" + length));
            byte[] bytes = new byte[length]; random.nextBytes(bytes);
            Host.ConsoleLog log = new Host.ConsoleLog(dir, limit);
            for (int offset = 0; offset < length;) {
                int n = Math.min(length - offset, 1 + random.nextInt(23)); log.write(bytes, offset, n); offset += n;
                check(Files.size(dir.resolve("console.log")) <= limit, "current log bounded during write");
                check(!Files.exists(dir.resolve("console.previous.log")) || Files.size(dir.resolve("console.previous.log")) <= limit,
                        "previous log bounded during write");
            }
            log.close(); log.close();
            rejected(() -> log.write(1), "writes after close rejected");
            int remaining = length == 0 ? 0 : (length - 1) % limit + 1;
            check(Arrays.equals(Files.readAllBytes(dir.resolve("console.log")), Arrays.copyOfRange(bytes, length - remaining, length)), "exact final byte segment");
            Path previous = dir.resolve("console.previous.log");
            if (length > limit) check(Arrays.equals(Files.readAllBytes(previous),
                    Arrays.copyOfRange(bytes, length - remaining - limit, length - remaining)), "exact previous byte segment");
            else check(!Files.exists(previous), "no invented previous bytes");
        }
        Path restart = Files.createDirectory(root.resolve("restart"));
        byte[] message = "failure before restart\n".getBytes(StandardCharsets.UTF_8);
        try (var log = new Host.ConsoleLog(restart, 64)) { log.write(message); }
        try (var log = new Host.ConsoleLog(restart, 64)) { /* Reserve an empty new segment. */ }
        try (var log = new Host.ConsoleLog(restart, 64)) {
            check(Arrays.equals(Files.readAllBytes(restart.resolve("console.previous.log")), message), "empty restart keeps preceding failure");
            log.write("recovered\n".getBytes(StandardCharsets.UTF_8));
        }
        Path oversized = Files.createDirectory(root.resolve("oversized"));
        byte[] old = new byte[65]; Arrays.fill(old, (byte) 'x');
        Files.write(oversized.resolve("console.log"), old);
        Files.writeString(oversized.resolve("console.previous.log"), "previous");
        rejected(() -> new Host.ConsoleLog(oversized, 64), "old oversized log preserved, not silently truncated");
        check(Arrays.equals(Files.readAllBytes(oversized.resolve("console.log")), old)
                && Files.readString(oversized.resolve("console.previous.log")).equals("previous"), "oversized files unchanged");
        Path blocked = Files.createDirectory(root.resolve("blocked"));
        Files.createDirectory(blocked.resolve("console.previous.log"));
        Files.writeString(blocked.resolve("console.log"), "current");
        rejected(() -> new Host.ConsoleLog(blocked, 64), "directory cannot be overwritten by rotation");
        check(Files.readString(blocked.resolve("console.log")).equals("current"), "nonregular previous preserves current");
        Path data = root.resolve("status.json");
        check(Host.supervisedStatus(data) == null, "missing status is a heartbeat absence");
        Files.writeString(data, status(System.currentTimeMillis(), "running"));
        check(Host.supervisedStatus(data).contains("running"), "bounded status read");
        Files.write(data, new byte[Host.STATUS_BYTES + 1]);
        rejected(() -> Host.supervisedStatus(data), "oversized status read rejected");
        try {
            Path target = root.resolve("untouched.txt"); Files.writeString(target, "keep");
            Path linkDir = Files.createDirectory(root.resolve("links"));
            Files.createSymbolicLink(linkDir.resolve("console.log"), target);
            rejected(() -> new Host.ConsoleLog(linkDir, 64), "console symlink rejected");
            rejected(() -> Host.supervisedStatus(linkDir.resolve("console.log")), "status symlink rejected");
            check(Files.readString(target).equals("keep"), "symlink target untouched");
            Path parent = root.resolve("parent-link"); Files.createSymbolicLink(parent, restart);
            rejected(() -> new Host.ConsoleLog(parent, 64), "symlink parent rejected");
        } catch (UnsupportedOperationException | FileSystemException unavailable) {
            System.out.println("SKIP unavailable symlink fixture: " + unavailable.getClass().getSimpleName());
        }
    }

    static final class CountingSink extends OutputStream {
        long bytes; int largest; boolean closed; final CRC32 crc = new CRC32();
        @Override public void write(int value) { bytes++; crc.update(value); largest = Math.max(largest, 1); }
        @Override public void write(byte[] b, int off, int len) { bytes += len; largest = Math.max(largest, len); crc.update(b, off, len); }
        @Override public void close() { closed = true; }
    }
    static final class FaultSink extends OutputStream {
        final String mode; final IOException problem = new IOException("injected disk quota exceeded"); int writes, flushes, closes;
        FaultSink(String mode) { this.mode = mode; }
        @Override public void write(int value) throws IOException { write(new byte[] {(byte)value}); }
        @Override public void write(byte[] b, int off, int n) throws IOException { writes++; if (mode.equals("write")) throw problem; }
        @Override public void flush() throws IOException { flushes++; if (mode.equals("flush")) throw problem; }
        @Override public void close() throws IOException { closes++; if (mode.equals("close") || mode.equals("write")) throw new IOException("injected close failure"); }
    }
    private static void capture() throws Exception {
        byte[] bytes = new byte[2 * 1024 * 1024 + 3]; new Random(81623).nextBytes(bytes);
        CRC32 oracle = new CRC32(); oracle.update(bytes);
        CountingSink screen = new CountingSink(), file = new CountingSink();
        Host.ConsoleCapture c = new Host.ConsoleCapture(new ByteArrayInputStream(bytes), screen, file, null); c.run();
        check(c.failure == null && screen.bytes == bytes.length && file.bytes == bytes.length, "both streams receive exact byte count");
        check(screen.crc.getValue() == oracle.getValue() && file.crc.getValue() == oracle.getValue(), "opaque bytes not line-decoded or rewritten");
        check(screen.largest <= 8192 && file.largest <= 8192 && !screen.closed && file.closed, "bounded buffer and stream ownership");
        for (String mode : List.of("write", "flush", "close")) {
            screen = new CountingSink(); FaultSink failure = new FaultSink(mode);
            c = new Host.ConsoleCapture(new ByteArrayInputStream(bytes), screen, failure, null); c.run();
            check(screen.bytes == bytes.length && screen.crc.getValue() == oracle.getValue(), "failed local sink must not stop drain: " + mode);
            check(c.failure != null && failure.closes == 1 && !screen.closed, "failure recorded and local closed once: " + mode);
            if (!mode.equals("close")) check(failure.writes == 1 && c.failure.getCause() == failure.problem, "first error retained, no retry storm");
        }
        FaultSink mirror = new FaultSink("write"); file = new CountingSink();
        c = new Host.ConsoleCapture(new ByteArrayInputStream(bytes), mirror, file, null); c.run();
        check(c.failure != null && file.bytes == bytes.length && file.closed && mirror.closes == 0, "mirror failure still drains to file");
        FaultSink printFault = new FaultSink("write"); PrintStream print = new PrintStream(printFault); file = new CountingSink();
        c = new Host.ConsoleCapture(new ByteArrayInputStream(bytes), print, file, null); c.run();
        check(c.failure != null && file.bytes == bytes.length, "PrintStream swallowed error is detected");
        boolean[] closed = {false};
        InputStream broken = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("injected child read failure"); }
            @Override public void close() { closed[0] = true; }
        };
        file = new CountingSink(); c = new Host.ConsoleCapture(broken, new CountingSink(), file, null); c.run();
        check(c.failure != null && closed[0] && file.closed, "input failure closes resources");
    }

    private static List<String> command(String mode) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", Host.isWindows() ? "java.exe" : "java").toString();
        String classes = Path.of(SupervisorTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        return List.of(java, "-Xmx64m", "-XX:ActiveProcessorCount=1", "-cp", classes, "SupervisorTest", "child", mode);
    }
    private static void processDrain(Path root) throws Exception {
        Process child = new ProcessBuilder(command("flood")).directory(root.toFile()).redirectError(ProcessBuilder.Redirect.INHERIT).start(); processes++;
        CountingSink mirror = new CountingSink(); FaultSink failed = new FaultSink("write");
        Host.ConsoleCapture capture = new Host.ConsoleCapture(child.getInputStream(), mirror, failed, null);
        Thread pump = Thread.ofPlatform().start(capture);
        try {
            check(child.waitFor(15, TimeUnit.SECONDS), "faulted file must not wedge real child output pipe");
            pump.join(3000);
            check(!pump.isAlive() && child.exitValue() == 0, "flood producer and drain complete");
            check(mirror.bytes == 24L * 1024 * 1024 && capture.failure != null && failed.writes == 1, "all 24 MiB drained despite disk error");
        } finally { child.destroyForcibly(); child.waitFor(5, TimeUnit.SECONDS); pump.join(3000); }
    }
    private static void supervisedChildren(Path root) throws Exception {
        PrintStream oldOut = System.out, oldErr = System.err; InputStream oldIn = System.in;
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        try (PrintStream discard = new PrintStream(OutputStream.nullOutputStream()); PrintStream diagnostics = new PrintStream(errors)) {
            System.setOut(discard); System.setErr(diagnostics);
            for (String mode : List.of("healthy", "failed", "stalled", "missing", "oversized", "ignore-stop")) {
                Path dir = Files.createDirectory(root.resolve("child-" + mode)); Path server = Files.createDirectory(dir.resolve("server"));
                System.setIn(new ByteArrayInputStream(new byte[0])); long start = System.nanoTime();
                if (mode.equals("healthy")) { Host.supervise(server, dir, command(mode), FAST); checks++; }
                else rejected(() -> Host.supervise(server, dir, command(mode), FAST), "failed supervision cannot be reported successful: " + mode);
                processes++;
                check(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 20, "bounded synthetic supervision: " + mode);
                check(!Files.exists(dir.resolve("control.properties")), "private control metadata removed: " + mode);
                long pid = Long.parseLong(Files.readString(server.resolve("child.pid")));
                check(ProcessHandle.of(pid).map(p -> !p.isAlive()).orElse(true), "no orphan child: " + mode);
                if (!mode.equals("healthy") && !mode.equals("ignore-stop"))
                    check(Files.readString(server.resolve("stop.received")).equals("stop"), "graceful stop received: " + mode);
                if (mode.equals("ignore-stop")) check(!Files.exists(server.resolve("stop.received")), "ignored stop required termination");
            }
            check(errors.toString(StandardCharsets.UTF_8).contains("Final checkpoint is not guaranteed"), "termination cannot masquerade as saved state");
        } finally { System.setOut(oldOut); System.setErr(oldErr); System.setIn(oldIn); }
    }
    private static void child(String mode) throws Exception {
        if (mode.equals("flood")) {
            byte[] chunk = new byte[8192]; Arrays.fill(chunk, (byte) 'x');
            for (int i = 0; i < 3072; i++) System.out.write(chunk);
            System.out.flush(); if (System.out.checkError()) System.exit(3); return;
        }
        Files.writeString(Path.of("child.pid"), Long.toString(ProcessHandle.current().pid()));
        Path path = Path.of("plugins/BotsClustersMC/status.json"); Files.createDirectories(path.getParent());
        if (mode.equals("healthy")) {
            for (int i = 0; i < 15; i++) { Host.text(path, status(System.currentTimeMillis(), i % 2 == 0 ? "running" : "paused")); System.out.println("healthy " + i); Thread.sleep(100); }
            return;
        }
        if (mode.equals("oversized")) Files.write(path, new byte[Host.STATUS_BYTES + 1]);
        else if (!mode.equals("missing")) Host.text(path, status(System.currentTimeMillis(), mode.equals("failed") ? "failed" : "running"));
        if (mode.equals("ignore-stop")) { Thread.sleep(30000); return; }
        try (var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) if (line.equals("stop")) { Files.writeString(Path.of("stop.received"), line); System.out.println("clean stop acknowledged"); return; }
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("child")) { child(args[1]); return; }
        Path root = Files.createTempDirectory("bcmc-supervisor-test-");
        try { health(); logs(root); capture(); processDrain(root); supervisedChildren(root); }
        finally { try (var paths = Files.walk(root)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); } }
        System.out.println("PASS supervisor I/O/health: " + checks + " checks; " + processes + " synthetic child JVMs; no Minecraft/Academy changes");
    }
}
