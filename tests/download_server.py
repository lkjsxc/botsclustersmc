#!/usr/bin/env python3
"""Download one explicitly named Paper test version; never update production pins."""
import argparse, hashlib, json, os, subprocess, urllib.request
from pathlib import Path

USER_AGENT='botsclustersmc-compatibility/0.6.0 (https://github.com/lkjsxc/botsclustersmc)'
def request(url):
    return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':USER_AGENT}),timeout=180)
def main():
    p=argparse.ArgumentParser();p.add_argument('version');p.add_argument('directory',type=Path);args=p.parse_args()
    directory=args.directory.resolve();directory.mkdir(parents=True,exist_ok=False)
    with request(f'https://fill.papermc.io/v3/projects/paper/versions/{args.version}/builds') as response: builds=json.load(response)
    if not isinstance(builds,list): raise RuntimeError(f'Invalid build listing: {builds}')
    stable=[b for b in builds if b.get('channel')=='STABLE']
    if not stable: raise RuntimeError(f'No stable Paper build for exactly {args.version}; no fallback to another version')
    build=max(stable,key=lambda b:b['id']);download=build['downloads']['server:default']
    (directory/'download.json').write_text(json.dumps(build,indent=2))
    checksum=download.get('checksums',{}).get('sha256')
    if not checksum: raise RuntimeError(f'No published SHA256 in selected download: {download}')
    target=directory/'server.jar';digest=hashlib.sha256()
    with request(download['url']) as response,target.open('wb') as output:
        while block:=response.read(1024*1024): output.write(block);digest.update(block)
    if digest.hexdigest()!=checksum: raise RuntimeError('Published server checksum did not match')
    print(f'EXACT SERVER: Paper {args.version} build {build["id"]}, sha256={checksum}',flush=True)
    subprocess.run([os.environ.get('JAVA_BIN','java'),'-Xmx1G','-Dpaperclip.patchonly=true','-jar',str(target)],cwd=directory,check=True)
if __name__=='__main__':main()
