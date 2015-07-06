import sys
import os
import subprocess

# From http://iconhandbook.co.uk/reference/chart/android

resolutions = {'ldpi': 120,
                'mdpi': 160,
                'hdpi' : 240,
                'xhdpi' : 320,
                'xxhdpi' : 480,
                'xxxhdpi' : 640}

launcher_sizes = {'mdpi': 48,
                  'hdpi': 72,
                  'xhdpi': 96,
                  'xxhdpi': 144,
                  'xxxhdpi': 192}

notification_sizes = {'mdpi': 22,
                  'hdpi': 36,
                  'xhdpi': 48,
                  'xxhdpi': 72,
                  'xxxhdpi': 96}

usage = "Usage: python3 export.py <svg file> [launcher|notification|image]"
if len(sys.argv) < 3:
    print(usage)
    exit(1)

file_name = sys.argv[1]
out_file_name, _ = os.path.splitext(file_name)
out_file_name += '.png'

sizes = None
image_type = sys.argv[2]
if image_type == "launcher":
    sizes = launcher_sizes
elif image_type == "notification":
    sizes = notification_sizes
elif image_type == "image":
    sizes = resolutions
else:
    print("Unrecognised option [" + image_type + "]")
    print(usage)
    exit(1)

for name in sizes:
    folder_name = "drawable-" + name
    sz = sizes[name]
    args = []
    args.append('inkscape')
    args.append('--export-png=../' + folder_name + '/' + out_file_name)
    if image_type == "image":
        args.append('--export-dpi=' + str(sz))
    else:
        args.append('--export-width=' + str(sz))
        args.append('--export-height=' + str(sz))
    args.append(file_name)
    subprocess.call(args)
