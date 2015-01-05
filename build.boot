(set-env!
 :dependencies '[[adzerk/bootlaces "0.1.6-SNAPSHOT" :scope "test"]
                 [com.stuartsierra/component "0.2.2"]
                 [org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/clojurescript "0.0-2411"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/tools.reader "0.8.9"]
                 [prismatic/plumbing "0.3.5"]
                 [prismatic/schema "0.3.2"]
                 [quile/component-cljs "0.2.2"]]
 :source-paths #{"src"}
 :resource-paths #(conj % "resources"))

(require
 '[adzerk.bootlaces :refer :all])

(def +version+ "0.1.0-SNAPSHOT")
(bootlaces! +version+)

(task-options!
 pom {:project 'pleasetrythisathome/components
      :version +version+
      :description "components"
      :url "https://github.com/pleasetrythisathome/components"
      :scm {:url "https://github.com/pleasetrythisathome/components"}})
