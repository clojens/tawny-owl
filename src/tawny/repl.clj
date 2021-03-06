;; The contents of this file are subject to the LGPL License, Version 3.0.

;; Copyright (C) 2012, Newcastle University

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Lesser General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Lesser General Public License for more details.

;; You should have received a copy of the GNU Lesser General Public License
;; along with this program.  If not, see http://www.gnu.org/licenses/.


(ns tawny.repl
  (:require [tawny.owl :as o]
            [tawny.lookup]
            [tawny.render]
            [clojure.pprint]
            )
  (:import [java.io StringWriter PrintWriter])
  )

;; do a documentation formatter first
;; want to have two entry points -- "update all documentation" which does 
;; everything in a namespace. And everything for a single symbol, which 
;; I can hook into the macros. 

;; this does the job of adding metadata to an existing symbol. 
;; (intern *ns* (vary-meta 'test-without-doc assoc :doc "Now we have documentation"))


(defn fetch-doc 
  ([owlobject]
     (fetch-doc owlobject (o/get-current-ontology)))
  ([owlobject ontology]
     (binding [tawny.lookup/all-iri-to-var-cache
                (tawny.lookup/all-iri-to-var)]
       (let [annotation (.getAnnotations owlobject ontology)
             label
             (filter 
              #(-> %
                   (.getProperty)
                   (.isLabel))
              annotation)

             comment
             (filter
              #(-> %
                   (.getProperty)
                   (.isComment))
              annotation)
             
             iri (-> owlobject
                     (.getIRI)
                     (.toURI)
                     (.toString))
             
             writer (StringWriter.)
             pwriter (PrintWriter. writer)
             line (fn [& args]
                    (.println pwriter 
                              (str (apply str args))))]

         (line "")

         (line 

          (.toString (.getEntityType owlobject))
          ": "
          (tawny.lookup/var-maybe-qualified-str
           (get
            (tawny.lookup/all-iri-to-var) iri)))

         (line "IRI: " iri)
         (line "Labels:")
         (doseq [l label]
           (line "\t" (.getValue l)))

         (line "Comments:")
         (doseq [c comment]
           (line "\t" (.getValue c)))
         
         
         (line "Full Definition:")
         (o/with-ontology ontology
           (clojure.pprint/pprint
            (tawny.render/as-form owlobject)
            writer))

         (.toString writer)))))

(defn print-doc
  ([owlobject]
     (println (fetch-doc owlobject)))

  ([owlobject ontology]
     (println (fetch-doc owlobject ontology))))

(defn print-ns-doc
  ([]
     (print-ns-doc *ns*))
  ([ns]
     (binding [tawny.lookup/all-iri-to-var-cache
                (tawny.lookup/all-iri-to-var)]
       (doseq [v
               (vals
                (tawny.lookup/iri-to-var ns))]
         (println (fetch-doc (var-get v)))))))


(defn update-var-doc
  "Updates the documentation on a var containing a OWLObject"
  [var]
  (alter-meta!
   var
   (fn [meta var]
     (assoc meta
       :doc (fetch-doc
             (var-get var))))
   var))

(defmacro update-doc
  [name]
  `(update-var-doc (var ~name)))

(defn update-ns-doc
  ([]
     (update-ns-doc *ns*))
  ([ns]
     (binding [tawny.lookup/all-iri-to-var-cache
                (tawny.lookup/all-iri-to-var)]
       (doseq [v
               (vals
                (tawny.lookup/iri-to-var ns))]
         (update-var-doc v)))))
