# Renders the Play Store icon and feature graphic from the app icon and fonts.
# Usage: python3 tools/play-graphics/render.py <repo root>  (needs Pillow)
import sys
from PIL import Image, ImageDraw, ImageFont
root = sys.argv[1]
BG = (0x0E, 0x0F, 0x12)
LIME = (0xC6, 0xFF, 0x00)
WHITE = (0xF2, 0xF3, 0xF5)
GREY = (0xA0, 0xA4, 0xAE)
# Dumbbell from app/src/main/res/drawable/ic_launcher_foreground.xml (108-unit viewport).
RECTS = [(39, 52, 69, 56, 0), (32.5, 39, 39.5, 69, 1.5), (68.5, 39, 75.5, 69, 1.5), (25.5, 45, 31.5, 63, 1.5), (76.5, 45, 82.5, 63, 1.5)]

def dumbbell(draw, ox, oy, scale):
    for x0, y0, x1, y1, r in RECTS:
        draw.rounded_rectangle([ox + x0 * scale, oy + y0 * scale, ox + x1 * scale, oy + y1 * scale], radius=r * scale, fill=LIME)

SS = 4  # supersample for smooth edges
icon = Image.new("RGB", (512 * SS, 512 * SS), BG)
dumbbell(ImageDraw.Draw(icon), 0, 0, 512 * SS / 108)
icon.resize((512, 512), Image.LANCZOS).save(f"{root}/docs/play-store/graphics/icon-512.png")

W, H = 1024, 500
fg = Image.new("RGB", (W * SS, H * SS), BG)
d = ImageDraw.Draw(fg)
dumbbell(d, 40 * SS, (H * SS - 108 * 4.2 * SS) / 2, 4.2 * SS)
fonts = f"{root}/core/designsystem/src/main/res/font"
title = ImageFont.truetype(f"{fonts}/bebas_neue_regular.ttf", 150 * SS)
body = ImageFont.truetype(f"{fonts}/inter_variable.ttf", 34 * SS)
small = ImageFont.truetype(f"{fonts}/inter_variable.ttf", 24 * SS)
x = 500 * SS
d.text((x, 95 * SS), "JALILGYM", font=title, fill=WHITE)
d.text((x, 265 * SS), "Train with structure.", font=body, fill=LIME)
d.text((x, 320 * SS), "Programs · Logging · Nutrition", font=body, fill=GREY)
d.text((x, 410 * SS), "Created by CH.Djalil", font=small, fill=GREY)
fg.resize((W, H), Image.LANCZOS).save(f"{root}/docs/play-store/graphics/feature-graphic-1024x500.png")
