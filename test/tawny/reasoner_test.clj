;; The contents of this file are subject to the LGPL License, Version 3.0.

;; Copyright (C) 2011, Newcastle University

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


(ns tawny.reasoner-test
  (:refer-clojure :exclude [some only comment])
  (:require [tawny.owl :as o]
            [tawny.reasoner :as r])
  [:use clojure.test])


(defn createtestontology[]
  (o/ontology 
   :iri "http://iri/"
   :prefix "iri:"))

(defn createandsavefixture[test]
  (binding
      [r/*reasoner-progress-monitor*
       (atom r/reasoner-progress-monitor-silent)]
    (o/with-ontology
      (createtestontology)
      (test)
      (o/save-ontology "test-reasoner.omn"))))



;; this isn't working and I really don't know why
;; it seems to work on lein test but kills all tests
;; when run in repl with clojure-test mode. 

;; works fine in fixture above so leave it there
(defn reasoner-gui-fixture [tests]
  (binding [r/*reasoner-progress-monitor*
            r/reasoner-progress-monitor-text]
    (tests)))



(use-fixtures
 ;;:once reasoner-gui-fixture
 :each createandsavefixture)


(defn with-ontology []
  (is
   (not
    (nil? (o/get-current-ontology)))))

(defn ontology-abc []
  (o/owlclass "a")
  (o/owlclass "b")
  (o/owlclass "c" :subclass "a" "b"))

(defn ontology-abc-indc []
  (ontology-abc)
  (o/individual "indC" :type "c"))


(defn ontology-abc-reasoning []
  ;; simple ontology -- c should be reasoned to be a subclass of a.
  (o/owlclass "a"
              :equivalent 
              (o/object-some "p" "b"))
  (o/owlclass "b")
  (o/owlclass "c"
              :subclass 
              (o/object-some "p" "b")))


(defn far-reasoner [func reasonerlist]
  ;; lazy sequences are crazy
  (doall
   (map
    (fn [x]
      (r/reasoner-factory x)
      (func))
    reasonerlist)))

;; for all reasoners
(defn far [func]
  (far-reasoner func
                '(:elk :hermit)))

;; for all dl reasoners
(defn fadlr [func]
  (far-reasoner func
                '(:hermit)))
;; (map
;;  (fn [x]
;;    (r/reasoner-factory x)
;;    (r/consistent?))
;;  '(:elk :hermit))


(deftest no-reasoner-set
  (is
   (thrown? 
    IllegalStateException
    (dosync 
     (ref-set r/vreasoner-factory nil)
     (r/reasoner)))))

(deftest empty-consistent? 
  ;; empty only is consistent
  (is
   (every?
    identity
    (far #(r/consistent?)))))


(deftest ind-consistent? 
  ;; with individual
  (is
   (every?
    identity
    (do
      (ontology-abc-indc)
      (far #(r/consistent?))))))


(deftest simple-consistent []
  ;; simple ontology without ind
  (is
   (every?
    identity
    (do
      (ontology-abc)
      (far #(r/consistent?))))))

(deftest disjoint-consistent []
  ;; without ind -- should be incoherent
  (is
   (every?
    complement
    (do
      (ontology-abc)
      (#'o/disjointclasses "a" "b")
      (far #(r/consistent?))))))

  
(deftest disjoint-and-individual []
  ;; now ontology should be inconsistent also
  (is
   (every?
    complement
    (do
      (ontology-abc-indc)
      (#'o/disjointclasses "a" "b")
      (far #(r/consistent?))))))

(deftest unsatisfiable []

  (is
   (every?
    #(= 0 (count %))
    (do (ontology-abc)
        (far #(r/unsatisfiable)))))

  (is
   (every?
    #(= 1 (count %))
    (do
      (ontology-abc)
      (#'o/disjointclasses "a" "b")
      (far #(r/unsatisfiable))))))

;; had lots of problems getting this working so lets try with a single reasoner
(deftest single-coherent
  (is
   (do 
     (r/reasoner-factory :hermit)
     (ontology-abc)
     (r/coherent?))))


(deftest coherent 
  (is
   (every?
    identity
    (do
      (ontology-abc)
      (far #(do 
              (r/coherent?)))))))


(deftest incoherent
  (is
   (every?
    not
    (do
      (ontology-abc)
      (#'o/disjointclasses "a" "b")
      (far #(r/coherent?))))))


(deftest isuperclass?
  (is
   (every? 
    identity
    (do 
      (ontology-abc-reasoning)
      (far #(r/isuperclass?
             (o/owlclass "c") 
             (o/owlclass "a")))))))

(deftest isubclass?
  (is
   (every?
    identity
    (do
      (ontology-abc-reasoning)
      (far #(r/isubclass?
             (o/owlclass "a")
             (o/owlclass "c"))))
    )))




(deftest with-probe-axioms
  ;; add a disjoint see whether it breaks
  (is
   (every?
    not
    (do
      (ontology-abc)
      (o/with-probe-axioms
        [a (#'o/disjointclasses "a" "b")]
        (o/save-ontology "test-probe.omn" :omn)
        (doall (far #(r/coherent?)))))))

  ;; add a disjoint test whether it breaks after
  (is 
   (every?
    identity
    (do 
      (ontology-abc)
      (o/with-probe-axioms
        [a (#'o/disjointclasses "a" "b")])
      (o/save-ontology "test-probe-after.omn" :omn)
      (doall (far #(r/coherent?)))))))


(deftest reasoner-gui-maybe
  (is
   (instance? org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor
              (r/reasoner-progress-monitor-gui-maybe)) ()))
