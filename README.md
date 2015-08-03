# ipython-clojure
A IPython kernel written in Clojure, this will let you run Clojure code from the IPython console and notebook

![ipython-clojure](https://raw.github.com/roryk/ipython-clojure/master/images/demo.gif)

## using it
1. run make
2. make a new profile with `ipython profile create clojure`
3. add these lines to your ipython_config.py located in .ipython/profile_clojure/ipython_config.py

  ```
  # Set the kernel command.
  c = get_config()
  # replace $(IPYTHON_CLOJURE_ROOT) with the actual path on your disk
  c.KernelManager.kernel_cmd = ["${IPYTHON_CLOJURE_ROOT}/bin/ipython-clojure",
                              "{connection_file}"] 

  # Disable authentication.
  c.Session.key = b''
  c.Session.keyfile = b''
  ```

4. If you want to be able to use notebooks, do the following as well:
  
  (1) Run `mkdir -p ~/.ipython/kernels/clojure`
  
  (2) Make a new file named 'kernel.json' under ~/.ipython/kernels/clojure, and add the following lines:
  
  ```
  {
    "argv": ["${IPYTHON_CLOJURE_ROOT}/bin/ipython-clojure", "{connection_file}"],
    "display_name": "Clojure",
    "language": "clojure"
  }
  ```
  
  (3) Edit `~/.ipython/profile_clojure/static/custom/custom.js` and add the following lines:
  
  ```
  define([
    'base/js/namespace',
    'base/js/events'
    ],
    function(IPython, events) {
        events.on("app_initialized.NotebookApp",
            function () {
                IPython.Cell.options_default.cm_config.indentUnit = 2;
            }
        );
    }
  );
  ```
  
5. run the repl with `ipython console --profile clojure`
  
  or run the notebook with `ipython notebook --profile clojure`

## status
Currently supports code execution and auto-complete. More implementations of the iPython messaging protocols to come.
