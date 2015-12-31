(set-env!
  :source-paths #{"src"}
  :dependencies '[[adzerk/bootlaces "0.1.12" :scope "test"]])

(require
  '[boot.util :as util]
  '[adzerk.bootlaces :refer :all :exclude [build-jar] :as laces]
  '[clojure.java.io :as io])

(def +version+ "0.1.0")

(bootlaces! +version+)

(task-options!
  pom {:project 'tcrawley/boot-verify
       :version +version+
       :description "Boot task for verifying dependency signatures"
       :url "https://github.com/tobias/boot-verify"
       :scm {:name "git"
             :url "https://github.com/tobias/boot-verify"}
       :license {"Eclipse Public License - v 1.0"
                 "http://www.eclipse.org/legal/epl-v10.html"}})

(def in-pod-dependencies
  [['tcrawley/boot-verify +version+]])

(deftask write-pod-dependencies []
  (with-pre-wrap fileset
    (let [tgt (tmp-dir!)]
      (util/info "Writing in-pod-dependencies.edn...\n")
      (spit (io/file tgt "in-pod-dependencies.edn")
        (pr-str in-pod-dependencies))
      (-> fileset (add-resource tgt) commit!))))

(deftask build-jar [] (comp (write-pod-dependencies) (laces/build-jar)))

(deftask release [] (comp (write-pod-dependencies) (pom) (jar) (push-release)))
