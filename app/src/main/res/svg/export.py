import sys
import os
import subprocess

file_name = sys.argv[1]
out_file_name, _ = os.path.splitext(file_name)
out_file_name += '.png'
out_file_name = '../' + out_file_name

subprocess.call(['rm', '-rf', 'drawable*'])

# From http://developer.android.com/guide/practices/screens_support.html
"""
resolutions = {'ldpi': 120,
                'mdpi': 160,
                'hdpi' : 240,
                'xhdpi' : 320,
                'xxhdpi' : 480,
                'xxxhdpi' : 640}

for name in resolutions:
    folder_name = "drawable-" + name
    res = resolutions[name]
    os.makedirs(folder_name)
    subprocess.call(['inkscape', 
                     '--export-png=' + folder_name + '/' + out_file_name,
                     '--export-dpi=' + str(res),
                     file_name])
"""

launcher_sizes = {'mdpi': 48,
                  'hdpi': 72,
                  'xhdpi': 96,
                  'xxhdpi': 144,
                  'xxxhdpi': 192}


for name in launcher_sizes:
    folder_name = "drawable-" + name
    sz = launcher_sizes[name]
    os.makedirs(folder_name)
    subprocess.call(['inkscape', 
                     '--export-png=' + folder_name + '/' + out_file_name,
                     '--export-width=' + str(sz),
                     '--export-height=' + str(sz),
                     file_name])
