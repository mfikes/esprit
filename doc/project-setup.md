# Project Setup

Eventually, you will want to move away from the REPL and start building up firmware proper. Additionally, as Esprit will work on any ESP32 with at least 8MB of flash, you may want to create your own printed circuit board (PCB) for your project. Esprit provides functionality to support this kind of development. An example skeleton project is setup [here](https://github.com/kiranshila/esprit-skeleton) as a reference.

## REPL
The Esprit REPL must be explicitly included in your project to enable REPL support at runtime. Simply `(:require [esprit.repl])` in your project's namespace somewhere. Note: if your project is built with `:advanced` optimizations, the REPL code will probably be removed.

## Live Coding

### CLI Tools
To connect to the running REPL using the Clojure CLI tools, simply run the following, replacing the endpoint address with the correct IP.
```bash
clj -m cljs.main -re esprit -ro '{:endpoint-address "10.0.0.1"}' -r
```

Additionally, the endpoint address can be provided in a `config.edn` file in the classpath with the `:endpoint-address` setting. Then, the connection command would be
```bash
clj -m cljs.main -re esprit -r
```

### Emacs/CIDER
By leveraging CIDER's piggieback library, we can jack-in to a remote REPL by adding a custom cljs-init form. This can be set in the `cider-custom-cljs-repl-init-form` variable. This variable must be set to
```clojure
(do (require 'esprit.repl)
    (cider.piggieback/cljs-repl
     (esprit.repl/repl-env)))
```
Additionally, CIDER's default printing function must be changed to `pr` as we don't provide support for `cljs.pprint` at this time.
This is set in the `cider-print-fn` variable, and must be set to `'pr`. 

Finally, following `cider-jack-in-cljs`, the REPL environment is set to `custom`. 

An example `.dir-locals.el` would then be setup as

```lisp
((nil
  (cider-custom-cljs-repl-init-form . "(do (require 'esprit.repl)
                                           (cider.piggieback/cljs-repl
                                            (esprit.repl/repl-env)))")
  (cider-default-cljs-repl . custom)
  (cider-print-fn . pr)))
```

Esprit's REPL environment's endpoint address can be set in `config.edn` with the `:endpoint-address` setting. The IP of the running Esprit instance is displayed upon successful connection to WiFi.

### Other Editors
Contributions welcome!

## Board Configuration
Esprit provides a board customization mechanism that can setup various "board items". These items can be pins and peripherals. See [[esprit.board/board-item]] for the currently supported items.

The board file is simply an EDN file on your classpath, and is configured in `config.edn` by the `:board-file` setting. If this is not set, the default Esprit board pin definitions will be configured.

For example, say you create a board configuration named `my-board.edn` in your classpath with the following contents:

```clojure
{:my-library.file/my-led
 {:type :output-digital
  :pin "D20"
  :value 0}}
```

In `config.edn` (also in your classpath), you would then set

```clojure
{:board-file "my-board.edn"}
```

In your project or library, you can then use the `esprit.board/items` map to access the pre-initialized Espruino objects. For example, in `my-library/file.cljs` one could now use `my-led` as:

```clojure
(def my-led (::my-led esprit.board/items))
```

### Blinkenlights
By default, Esprit supports a few LEDs that provide indications for the different stages of the REPL (Read, Eval, and Print) as well as the connection status. To enable them, in your `board.edn` file, supply pin definitions for the following keywords:
* `:esprit.indicators/read-led`
* `:esprit.indicators/eval-led`
* `:esprit.indicators/print-led`
* `:esprit.indicators/conn-led`

The initial value for these LEDs depends on the board's hardware setup, but for the Esprit board with the LEDs in the open-drain configuration (logical low turns the LEDs on), the `board.edn` file would be set up as follows:

```clojure
{:esprit.indicators/read-led
 {:type :output-analog
  :pin "D14"
  :value 1}
 :esprit.indicators/eval-led
 {:type :output-analog
  :pin "D33"
  :value 0.2
  :freq 1}
 :esprit.indicators/print-led
 {:type :output-analog
  :pin "D32"
  :value 1}
 :esprit.indicators/conn-led
 {:type :output-analog
  :pin "D13"
  :value 1}}
```

Of course, if these pins are not provided in the `board.edn` file, the blinkenlight functionality is disabled. 
