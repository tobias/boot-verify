# tcrawley/boot-verify

Verifies signatures of dependencies, similar to `lein deps :tree`.

## Why?

In case you want to audit your dependency tree to ensure that all of
your dependencies are signed. 

## Usage

First, add it as a dependency in `build.boot`:

```clojure
(set-env! :dependencies '[[tcrawley/boot-verify "0.1.0" :scope "test"]
                          ...])
```
Then, require it:

```clojure
(require '[tcrawley.boot-verify :refer [verify]])
```

Lastly, use it:

```sh
$ boot verify
:signed        [org.immutant/web "2.1.1"]
:signed        [org.projectodd.wunderboss/wunderboss-web-undertow "0.10.0"]
:signed        [org.immutant/core "2.1.1"]
:signed        [org.clojure/java.classpath "0.2.2"]
:unsigned      [io.undertow/undertow-core "1.3.0.Beta9"]
:bad-signature [commons-fileupload "1.3"]
...
```

## License

Copyright (C) 2015 Tobias Crawley.

Licensed under the Eclipse Public License v1.0
