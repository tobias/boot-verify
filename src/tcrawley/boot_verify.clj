(ns tcrawley.boot-verify
  {:boot/export-tasks true}
  (:require [boot.core :as core]
            [boot.pod  :as pod]
            [boot.util :as util]
            [clojure.java.io :as io]))

(defn ^:private load-data [resource-name]
  (-> resource-name io/resource slurp read-string))

(defn ^:private in-pod-dependencies []
  (conj (load-data "in-pod-dependencies.edn") ['boot/aether core/*boot-version*]))

(def ^:private pod
  (delay
    (pod/make-pod
      (assoc pod/env :dependencies (in-pod-dependencies)))))

(core/deftask verify
  "Verify signatures of artifacts in the dependency tree"
  [P pods REGEX regex "The name filter used to select which pods to inspect."]
  (core/with-pass-thru [_]
    (let [pattern (or pods #"^core$")
          sort-pods #(sort-by (memfn getName) %)]
      (doseq [p (->> pattern pod/get-pods sort-pods)
              :let  [pod-name (.getName p)]
              :when (not (#{"worker" "aether"} pod-name))]
        (let [pod-env (if (= pod-name "core")
                        (core/get-env)
                        (pod/with-eval-in p boot.pod/env))]
          (when pods (util/info "\nPod: %s\n\n" pod-name))
          (pod/call-in* @pod
            ['tcrawley.boot-verify.in-pod/verify pod-env]))))))





