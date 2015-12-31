(ns tcrawley.boot-verify.in-pod
  (:require boot.aether
            [boot.from.io.aviso.ansi     :as ansi]
            [boot.gpg                    :as gpg]
            [boot.util                   :as util]
            [cemerick.pomegranate.aether :as aether])
  (:import org.sonatype.aether.resolution.DependencyResolutionException))

(defn dep-spec->map [dep]
  (let [[name version & rest] dep
        meta (apply hash-map rest)]
    (assoc meta :name name :version version)))

(defn map->dep-spec [{:keys [name version] :as m}]
  (into [name version] (apply concat (dissoc m :name :version))))

(declare check-signature)

(defn- ^{:boot/from :technomancy/leiningen} fetch-key
  [signature err]
  (if (or (re-find #"Can't check signature: public key not found" err)
        (re-find #"Can't check signature: No public key" err))
    (let [key (second (re-find #"using \w+ key ID (.+)" err))
          {:keys [exit]} (gpg/gpg "--recv-keys" "--" key)]
      (if (zero? exit)
        (check-signature signature)
        :no-key))
    :bad-signature))

(defn- ^{:boot/from :technomancy/leiningen} check-signature
  [signature]
  (let [{:keys [err exit]} (gpg/gpg "--verify" "--" (str signature))]
    (if (zero? exit)
      :signed ; TODO distinguish between signed and trusted
      (fetch-key signature err))))

(defn ignore-checksum [[name settings]]
  [name (assoc
          (if (string? settings) {:url settings} settings)
          :checksum :ignore)])

(defn ^{:boot/from :technomancy/leiningen} get-signature
  ;; TODO: check pom signature too
  [dep {:keys [repositories mirrors]}]
  (try
    (->> (aether/resolve-dependencies
           :repositories (map ignore-checksum repositories)
           :mirrors      mirrors
           :coordinates  [(-> dep
                            dep-spec->map
                            (assoc :extension "jar.asc")
                            map->dep-spec)])
      (aether/dependency-files)
      (filter #(.endsWith (.getName %) ".asc"))
      (first))
    (catch DependencyResolutionException _)))

(defn verify-dep [dep env]
  (if-let [signature (get-signature dep env)]
    (check-signature signature)
    :unsigned))

(def color-fns {:signed        ansi/bold-cyan
                :unsigned      ansi/bold-red
                :no-key        ansi/bold-yellow
                :bad-signature ansi/bold-yellow})

(defn print-verification [{:keys [result dep]}]
  (println ((color-fns result) (format "%-14s" result))
    (-> dep
      dep-spec->map
      (dissoc :exclusions :scope)
      map->dep-spec
      pr-str)))

(defn verify [env]
  (doseq [dep (boot.aether/resolve-dependencies env)]
    (print-verification (assoc dep :result (verify-dep (:dep dep) env)))))



