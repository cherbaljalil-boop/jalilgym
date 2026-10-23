import sys; sys.path.insert(0, sys.argv[1])
from body import *
def fig(regions, dx):
    s = [f'<g transform="translate({dx},0)">']
    for d in full([SILHOUETTE]): s.append(f'<path d="{d}" fill="#24272D"/>')
    s.append('<circle cx="50" cy="13" r="9" fill="#24272D"/>')
    colors = ["#C6FF00", "#7FA300", "#4F7CAC", "#E4FF8A", "#3A4A00", "#A6ABB3"]
    for i, (m, ps) in enumerate(regions.items()):
        for d in full(ps): s.append(f'<path d="{d}" fill="{colors[i % len(colors)]}" stroke="#0E0F12" stroke-width="0.6"/>')
    s.append('</g>'); return "\n".join(s)
svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 220 200" width="660" height="600" style="background:#0E0F12">{fig(FRONT,0)}{fig(BACK,120)}</svg>'
open(sys.argv[2], "w").write(svg)
