#!/usr/bin/env python3
"""Bounded, disposable compiler-level mutants for the conditional focus contract."""
from pathlib import Path
import json, os, subprocess, tempfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = os.environ.get("JAVA_BIN", "java")
JAVAC = str(Path(JAVA).with_name("javac")) if os.path.sep in JAVA else "javac"
TARGET = "core/src/org/botsclustersmc/core/Distribution.java"
MUTANTS = {
    "unweighted_world_entropy": (
        "worldProbability(p)*worldEntropy(p)", "worldEntropy(p)"),
    "opening_ghost_score": (
        "head==6||MenuFocus.worldBranch(a[6])?advantage:0", "advantage"),
    "missing_parent_world_derivative": (
        "(MenuFocus.worldBranch(j)?worldEntropy:0)+(Schema.slotActive(j)?childEntropy[j]:0)",
        "(Schema.slotActive(j)?childEntropy[j]:0)"),
    "unweighted_world_kl": (
        "result+=worldWeight*headDivergence(before,after,Task.offset(head),Schema.HEADS[head]);",
        "result+=headDivergence(before,after,Task.offset(head),Schema.HEADS[head]);"),
}

def run():
    paths = list((ROOT/"core/src").rglob("*.java"))
    paths += [ROOT/"training/src/org/botsclustersmc/training/Exploration.java"]
    paths += [ROOT/"tests/java/org/botsclustersmc/tests"/name for name in
              ("MenuPairRegression.java", "ConditionalDistributionTest.java")]
    source = (ROOT/TARGET).read_text()
    receipts = []
    for name, replacement in [("unchanged", None), *MUTANTS.items()]:
        with tempfile.TemporaryDirectory(prefix="bcmc-focus-mutant-") as temp:
            home = Path(temp)
            sources = []
            for path in paths:
                text = path.read_text()
                if path == ROOT/TARGET and replacement:
                    old, new = replacement
                    if source.count(old) != 1:
                        raise AssertionError(f"{name}: ambiguous mutation site")
                    text = source.replace(old, new)
                dest = home/path.relative_to(ROOT)
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_text(text)
                sources.append(str(dest))
            classes = home/"classes"; classes.mkdir()
            build = subprocess.run([JAVAC, "--release", "21", "-d", str(classes), *sources],
                                   capture_output=True, text=True, timeout=60)
            if build.returncode:
                raise AssertionError(f"{name}: compile failure is not a killed mutant: {build.stderr}")
            result = subprocess.run([JAVA, "-Xmx256m", "-cp", str(classes),
                                     "org.botsclustersmc.tests.ConditionalDistributionTest"],
                                    capture_output=True, text=True, timeout=90)
            passed = result.returncode == 0
            if passed != (replacement is None):
                raise AssertionError(f"{name}: unexpected exit {result.returncode}\n{result.stdout}\n{result.stderr}")
            if not passed and "AssertionError" not in result.stderr:
                raise AssertionError(f"{name}: failure was not a test assertion")
            receipts.append({"case": name, "exit": result.returncode,
                             "result": "pass" if passed else "killed-by-assertion",
                             "assertion": next((line for line in result.stderr.splitlines()
                                                if "AssertionError" in line), None)})
            print(json.dumps(receipts[-1]), flush=True)
    print("PASS unchanged implementation and four deliberately incorrect mutants", flush=True)

if __name__ == "__main__":
    run()
