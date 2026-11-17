from PIL import Image, ImageDraw
import os

img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))

# Fill main box faces [0..48, 0..24] with dark steel metal
for x in range(0, 48):
    for y in range(0, 24):
        # Base metallic dark slate
        color = (48, 54, 65, 255)
        # Border seam
        if x % 12 == 0 or x % 12 == 11 or y % 12 == 0 or y % 12 == 11:
            color = (25, 28, 35, 255)
        img.putpixel((x, y), color)

# Front face is (12..24, 12..24): Draw glowing red sensor eye
for x in range(15, 21):
    for y in range(15, 21):
        img.putpixel((x, y), (255, 50, 50, 255))
img.putpixel((17, 17), (255, 200, 200, 255))
img.putpixel((18, 17), (255, 200, 200, 255))
img.putpixel((17, 18), (255, 200, 200, 255))
img.putpixel((18, 18), (255, 200, 200, 255))

# Top face is (12..24, 0..12): Vent grill
for y in range(3, 9, 2):
    for x in range(14, 22):
        img.putpixel((x, y), (15, 18, 22, 255))

out_dir = r'mod/src/main/resources/assets/picking_hope_core/textures/entity'
os.makedirs(out_dir, exist_ok=True)
img.save(os.path.join(out_dir, 'security_drone.png'))
print('Generated security_drone.png successfully!')

# Generate fusion chip item texture (16x16)
chip_img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for x in range(2, 14):
    for y in range(2, 14):
        chip_img.putpixel((x, y), (30, 40, 55, 255))
# Golden pins
for x in (3, 6, 9, 12):
    chip_img.putpixel((x, 1), (255, 215, 0, 255))
    chip_img.putpixel((x, 14), (255, 215, 0, 255))
# Glowing cyan core
for x in range(6, 10):
    for y in range(6, 10):
        chip_img.putpixel((x, y), (0, 220, 255, 255))
chip_img.putpixel((7, 7), (220, 255, 255, 255))
chip_img.putpixel((8, 8), (220, 255, 255, 255))

item_dir = r'mod/src/main/resources/assets/picking_hope_core/textures/item'
os.makedirs(item_dir, exist_ok=True)
chip_img.save(os.path.join(item_dir, 'fusion_core_chip.png'))
print('Generated fusion_core_chip.png successfully!')
