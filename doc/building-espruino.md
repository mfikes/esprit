# Building Espruino

## Background
We need to make a custom build of Espruino for use with Esprit. The real trick is to set up Espruino such that it has enough space to hold `cljs.core` and all your program code when running a REPL. This ends up at least around 900KB.

Now for those unfamiliar, the ESP32 is configured with a partition table - allocating different sections of memory to the program code, non-volatile storage, the bootloader, etc. These partitions can be arbitrarily resized. Additionally, one can make partitions that don't mean anything to the ESP32, but can yield pointers in the C code that help manage the separate parts of memory.

As modern ESP32s ship with TONS of flash, we want to make the partition that Espruino calls `js_code` as large as possible to take full advantage of the chip. This presents a couple challenges.

1. Espruino makes an assumption in their ESP32 builds that the chips are all of the 4MB variant, so there are a few more things to change than just the partition table. 
2.  The build process for Espruino is a bit challenging to successfully complete, involving challenges that are sometimes a bit esoteric and a bit more advanced.

For the these reasons, we offer pre-built custom versions of the Espruino runtime for use with Esprit and these are directly accessed from the downloaded JAR and used by the Esprit flashing tools. 

But, if you want or need to build your own variant of Espruino, this page documents the process so that it is reproducible.

## Building
1. Clone the EspruinoBuildTools [repo][1]
2. In `esp32/build/app` modify `partitions_espruino.csv` as such
```patch
-js_code,data,0,0x320000,256K
-storage,data,0,0x360000,640K
+js_code,data,0,0x320000,3M
+storage,data,0,0x620000,1920K
```
> Note: We want the partition table to take up as much space as it can, 8MB as is the case for the ESP32. A normal data partition cannot be larger than 3MB as discussed [here][2]. It throws errors when trying to call `esp_partition_mmap`. We wish this could be bigger, as the the `js_code` partition is where our code will be burned.  We believe code can be stored and loaded from `storage` as well, but that hasn’t been sorted yet.
3. Now in the same directory, modify `sdkconfig`, the result of a`menuconfig` run, to let the compiler know that our target does in fact have 8MB of flash. This should also match the total size of the partition table.
```patch
-CONFIG_ESPTOOLPY_FLASHSIZE_4MB=y
-CONFIG_ESPTOOLPY_FLASHSIZE_8MB=
+CONFIG_ESPTOOLPY_FLASHSIZE_4MB=
+CONFIG_ESPTOOLPY_FLASHSIZE_8MB=y
-CONFIG_ESPTOOLPY_FLASHSIZE="4MB"
+CONFIG_ESPTOOLPY_FLASHSIZE="8MB"
```
 4. Run `. build-idf.sh` from the parent directory `esp32/build`, which should build the IDF as well as clone the main `Espruino` library. Be careful as these scripts move you around in directories, and can leave things in a weird state if they fail.
5. Move to the `esp32/build/Espruino` directory and check out the latest release branch, `git checkout RELEASE_2V06`. Note that currently the `master` branch has too many JS errors down the road when when trying to load in CLJS.
 6. In `esp32/build/Espruino/boards` modify `ESP32.py` to increase the number of 4096-length pages to match the `js_code` partition. 3MB/4096 = 768
```patch
-    'pages' : 64,
+    'pages' : 768,
```
7. In `esp32/build/Espruino/targets/esp32` modify `main.c` as follows in order to allocate more JavaScript space in Espruino (this is needed to accommodate loading all of `cljs.core`):
```patch
-  if(heapVars > 20000) heapVars = 20000;  //WROVER boards have much more RAM, so we set a limit
+  if(heapVars > 55000) heapVars = 55000;  //WROVER boards have much more RAM, so we set a limit
```
8. Back up to `esp32/build`, and then run `. build-partition.sh` and `. build-tgz.sh`. Those should have taken care of building everything, with results symlinked in the `esp32/build/Espruino` directory.
9. Grab the `bootloader.bin`, `espruino_esp32.bin` and `partitions_espruino.bin` from `esp32/build/Espruino` and move them to your project directory and use them as usual when [flashing][3].

[1]:	https://github.com/espruino/EspruinoBuildTools
[2]:	https://github.com/espressif/esp-idf/issues/1184
[3]:	https://cljdoc.org/d/esprit/esprit/CURRENT/doc/building-and-board-flashing