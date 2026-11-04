from PIL import Image, ImageDraw
import os

S = 1024
img = Image.new("RGBA", (S, S), "#07152f")
d = ImageDraw.Draw(img)

# Rounded-square background
d.rounded_rectangle((24, 24, 1000, 1000), radius=210, fill="#071b46")

# Circuit details
for x, y in [(120,180),(900,180),(120,820),(900,820)]:
    d.ellipse((x-18,y-18,x+18,y+18), fill="#00cfff")
    d.line((x,y,x+100,y-80), fill="#0879db", width=12)

# Main shield
d.polygon(
    [(512,110),(825,235),(790,580),(690,790),
     (512,910),(334,790),(234,580),(199,235)],
    fill="#0878ff"
)

# Inner shield
d.polygon(
    [(512,165),(765,270),(735,570),(650,745),
     (512,835),(374,745),(289,570),(259,270)],
    fill="#102b63"
)

# Android robot head
d.rounded_rectangle((370,330,654,555), radius=75, fill="#f4f8ff")
d.line((420,325,380,270), fill="#f4f8ff", width=22)
d.line((604,325,644,270), fill="#f4f8ff", width=22)
d.ellipse((425,405,465,445), fill="#102b63")
d.ellipse((559,405,599,445), fill="#102b63")

# Android body
d.rounded_rectangle((390,530,634,690), radius=35, fill="#f4f8ff")
d.rounded_rectangle((345,545,380,675), radius=18, fill="#f4f8ff")
d.rounded_rectangle((644,545,679,675), radius=18, fill="#f4f8ff")

# Blue padlock
d.arc((440,485,584,635), 180, 360, fill="#00d9ff", width=35)
d.rounded_rectangle((420,575,604,735), radius=35, fill="#0878ff")
d.ellipse((493,625,531,663), fill="#ffffff")
d.rounded_rectangle((505,650,519,695), radius=8, fill="#ffffff")

# Cyan orbital accent
d.arc((170,300,900,800), 195, 340, fill="#00d9ff", width=24)

# Export
os.makedirs("app/src/main/res/drawable", exist_ok=True)
os.makedirs("fastlane/metadata/android/en-US/images", exist_ok=True)

icon = img.resize((512,512), Image.Resampling.LANCZOS)
icon.save("fastlane/metadata/android/en-US/images/icon.png")

# Android vector-compatible PNG drawable
img.resize((432,432), Image.Resampling.LANCZOS).save(
    "app/src/main/res/drawable/appshield_icon.png"
)

print("AppShield icons generated successfully!")
