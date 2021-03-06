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


(ns tawny.owl-test
  (:import
   (org.semanticweb.owlapi.model OWLOntologyManager OWLOntology IRI
                                 OWLClassExpression OWLClass OWLAnnotation
                                 OWLIndividual OWLDatatype
                                 OWLNamedObject OWLOntologyID
                                 OWLAnnotationProperty OWLObjectProperty
                                 OWLDataProperty
                                 ))
  (:require [tawny.owl :as o]
            [tawny.util])
  [:use clojure.test])


(def testontology nil)

(defn createtestontology[]
  (alter-var-root
   #'testontology
   (fn [x]
     (o/ontology :iri "http://iri/" :prefix "iri"))))

(defn createandsavefixture[test]
  (reset! o/default-ontology-hook [])
;;             [tawny.debug]
;;  (tawny.util/add-hook o/default-ontology-hook
;;                       #(tawny.debug/tracing-println "default ontology used") )
  (o/with-ontology (createtestontology)
    (test)
    ;;(o/save-ontology "test.omn")
    )
  )

(use-fixtures :each createandsavefixture)


(deftest ontology
  (is true)
  (is
   (not
    (nil?
     (o/ontology
      :iri "http://iri/"
      :prefix "iri:"
      :comment "This is a comment"
      :versioinfo "This is some versioninfo")))))

(deftest defontology
  (is (not (nil? (o/get-current-ontology))))
  (is (= 0 (.getAxiomCount (#'o/get-current-ontology)))))

(deftest get-current-ontology
  (is (not (nil? (o/get-current-ontology)))))

(deftest get-iri
  (is (= "http://iri/"
         (.toString (o/get-iri (o/ontology :iri "http://iri/")))))
  (is (= "http://iri/"
         (.toString (o/get-iri)))))

(deftest get-current-iri
  (is (= "http://iri/" (.toString (#'o/get-current-iri)))))

(deftest get-current-prefix
  (is (= "iri:" (o/get-current-prefix))))

(deftest declare-classes
  (is
   (-> (o/declare-classes a)
       (nil?)
       (not))))


(deftest ontology-options

  (is
   (instance? clojure.lang.Ref
              (o/ontology-options testontology)))

  (is
   (let [options {:a 1 :b 2}]
     (dosync
      (alter (o/ontology-options testontology)
             merge options))

     (= @(o/ontology-options testontology)
            options)))

  (is
   (do
     ;; need a clean slate to start with!
     (reset! o/ontology-options-atom {})
     (o/ontology :iri "http://iri" :prefix "dfda:")
     (o/ontology-options)
     (o/ontology :iri "http://iri" :prefix "dfda:")
     (o/ontology-options)
     (o/ontology :iri "http://iri" :prefix "dfda:")
     (o/ontology-options)
     (= 1
          (count @o/ontology-options-atom)))))



(deftest save-ontology []
  (is (do (o/save-ontology "test.omn")
          true)))

(deftest iriforname []
  (is (= (.toString (#'o/iriforname "test"))
         "http://iri/#test")))

(deftest get-create-object-property []
  (is (instance? org.semanticweb.owlapi.model.OWLObjectProperty
                 (#'o/get-create-object-property "hello")))
  (is (instance? org.semanticweb.owlapi.model.OWLObjectProperty
                 (#'o/get-create-object-property
                  testontology "hello"))))


(deftest ensure-object-property []
  (is
   ;; check whether it makes an object out of a string
   (instance? org.semanticweb.owlapi.model.OWLObjectProperty
              (#'o/ensure-object-property "hello")))
  (is
   ;; check whether it makes keeps an object as an object
   (instance? org.semanticweb.owlapi.model.OWLObjectProperty
              (#'o/ensure-object-property
               (#'o/get-create-object-property "hello")))))

(deftest defoproperty
  (is
   (var?
    (o/defoproperty a))))


(deftest get-create-class []
  (is (instance? org.semanticweb.owlapi.model.OWLClass
                 (#'o/get-create-class "hello")))
  (is  (=  (.hashCode (#'o/get-create-class "hello"))
           (.hashCode (#'o/get-create-class "hello")))))


(deftest ensure-class []
  (is (instance? org.semanticweb.owlapi.model.OWLClassExpression
                 (#'o/ensure-class "hello")))
  (is (instance? org.semanticweb.owlapi.model.OWLClassExpression
                 (#'o/ensure-class (#'o/get-create-class "hello")))))


(deftest add-subclass
  (is
   (do
     (o/add-subclass "a" "b")
     (= 2 (.size (.getClassesInSignature
                  (o/get-current-ontology))))))
  (is
   (do
     (o/add-subclass "a" "b")
     (o/superclass? "a" "b")))
  )

(deftest add-equivalent []
  (let [equiv (#'o/add-equivalent
               (#'o/ensure-class "a")
               (list (#'o/ensure-class "b")))]
    (is (not (nil? equiv)))
))

(deftest add-class []
  (is (not
       (nil?
        (o/add-class "a")))))

(deftest add-haskey
  (is
   (not (nil?
         (do
           (o/with-probe-entities
             [p (o/objectproperty "p")
              c (o/owlclass "c")]
             (o/add-haskey c (list p))))))))

(deftest add-subpropertychain
  (is
   (not
    (nil?
     (do
       (let [p1 (o/objectproperty "p1")
             p2 (o/objectproperty "p2")
             p3 (o/objectproperty "p3")
             ]
         (o/add-subpropertychain
          p1 (list p2 p3)))))))

  (is
   (not
    (nil?
     (do
       (let [p1 (o/objectproperty "p1")
             p2 (o/objectproperty "p2")
             p3 (o/objectproperty "p3")
             ]
         (o/add-subpropertychain
          p1 (list [p2 p3])))))))

  (is
   (not
    (nil?
     (do (let [p1 (o/objectproperty "p1")
               p2 (o/objectproperty "p2")
               p3 (o/objectproperty "p3")
               p4 (o/objectproperty "p4")
               p5 (o/objectproperty "p5")
             ]
         (o/add-subpropertychain
          p1 (list p2 p3 [p4 p5])))))))
  )


(deftest objectproperty []
  (is (instance?
       org.semanticweb.owlapi.model.OWLObjectProperty
       (o/objectproperty "b")))
  )

(deftest owlsome []
  (is (not (nil? (o/owlsome (o/objectproperty "b") "a"))))
  (is (o/object-some
       testontology "has" "leg"))
  ;; failing test
  (is (thrown? clojure.lang.ArityException
               (o/owlsome (ensure-class "hasLeg")))))

(deftest owlonly []
  (is (not (nil? (o/only (o/objectproperty "b") "a")))))


(deftest owland []
  (is (not (nil? (o/object-and "a" "b"))))
  (is (instance?
       org.semanticweb.owlapi.model.OWLObjectIntersectionOf
       (o/owland "c" (o/owlclass "d"))))
  (is (thrown? IllegalArgumentException
               (o/owland)))
  (is (instance?
       org.semanticweb.owlapi.model.OWLDataIntersectionOf
       (o/owland o/xsd:integer o/xsd:float))))

(deftest owlor
  (is (not (nil? (o/object-or "a" "b"))))
  (is (instance?
       org.semanticweb.owlapi.model.OWLObjectUnionOf
       (o/owlor testontology "c" (o/owlclass "d"))))
  (is (thrown? IllegalArgumentException
               (o/owlor)))
  (is (instance?
       org.semanticweb.owlapi.model.OWLDataUnionOf
       (o/data-or testontology o/xsd:integer o/xsd:float)))
  (is (instance?
       org.semanticweb.owlapi.model.OWLDataUnionOf
       (o/owlor testontology o/xsd:integer o/xsd:float))))

(deftest owlnot
  (is (instance?
       org.semanticweb.owlapi.model.OWLObjectComplementOf
       (do
         (o/owlclass "b")
         (o/owlnot "b"))))
  (is (instance?
       org.semanticweb.owlapi.model.OWLObjectComplementOf
       (o/owlnot (o/owlclass "d"))))
  (is (thrown? IllegalArgumentException
               (o/owlnot)))
  (is (instance?
       org.semanticweb.owlapi.model.OWLDataComplementOf
       (o/data-not o/xsd:integer)))
  (is (instance?
       org.semanticweb.owlapi.model.OWLDataComplementOf
       (o/owlnot o/xsd:integer)))
)


(deftest someonly []
  (is
   (not
    (nil?
     (o/someonly
      (o/objectproperty "p") "a"))))


  (is
   (not
    (nil?
     (o/someonly (o/objectproperty "p") "a" "b")))))


(deftest disjointclasses []
  (is
   (do (#'o/disjointclasses "a" "b" "c")))

  (is
   (do (#'o/disjointclasses
        (o/owlclass "a") (o/owlclass "b")))))

(deftest owlclass
  (is (= 1
         (do (o/owlclass "test")
             (.size (.getClassesInSignature
                     (o/get-current-ontology))))))
  (is (instance? org.semanticweb.owlapi.model.OWLClass
                 (o/owlclass "test"))))


(deftest defclass
  (is (= 1
         (do (o/defclass a)
             (.size (.getClassesInSignature
                     (o/get-current-ontology))))))
  (is not
      ))



(defn- test-class-with-hierarchy
  "Some test classes

Assumes that fixture has been run
"
  []

  (o/owlclass "a")
  (o/owlclass "b" :subclass "a")
  (o/owlclass "c" :subclass "b")

  (o/owlclass "d")
  (o/owlclass "e" :subclass "b" "d")
  )
(deftest superclass? []
  (is (not
       (nil?
        (do
          (test-class-with-hierarchy)
          (o/direct-superclasses "c")))))
  (is (do
        (test-class-with-hierarchy)
        (o/superclass? "e" "a")))
  (is (not
       (do
         (test-class-with-hierarchy)
         (o/superclass? "c" "e")))))


(deftest subclass? []
  (is
   (do (test-class-with-hierarchy)
       (o/subclass? "a" "c")))
  (is
   (not
    (do (test-class-with-hierarchy)
        (o/subclass? "c" "e")))))


(deftest disjointclasses []
  (is (not (nil? (#'o/disjointclasses "a" "b" "c")))))

(deftest individual []
  (is (o/individual "ind"))
  (is (not (nil? (o/individual "indA" :type "a"))))
  (is (thrown? IllegalArgumentException
               (o/individual "indA" :nottypes "a"))))

(deftest defindividual []
  (is (do (o/defindividual testind)
          testind)))

(deftest remove-entity []
  (is
   (= 0
      (do
        (let [clazz (o/owlclass "a")]
          (o/remove-entity clazz)
          (.size (.getClassesInSignature
                  (o/get-current-ontology)))))))

  (is
   (= 0
      (do
        (let [prop (o/objectproperty "a")]
          (o/remove-entity prop)
          (.size (.getClassesInSignature
                  (o/get-current-ontology))))))))



(deftest with-probe-entities
  ;; are the classes created correctly
  (is
   (= 3
      (o/with-probe-entities
        [a (o/owlclass "a")
         b (o/owlclass "b")
         c (o/owlclass "c")]
        (-> (o/get-current-ontology)
            (.getClassesInSignature)
            (.size))
        )))

  (is
   (= 0
      (do
        (o/with-probe-entities
            [a (o/owlclass "a")
             b (o/owlclass "b")
             c (o/owlclass "c")
             ])
        ;; and have they gone again afterwards
        (-> (#'o/get-current-ontology)
            (.getClassesInSignature)
            (.size))))))


(defn ontology-c-with-two-parents []
  (o/owlclass "a")
  (o/owlclass "b")
  (o/owlclass "c" :subclass "a" "b"))


(deftest with-probe-axioms
  ;; add a disjoint see whether it breaks
  (is
   (= 1
      (do
        (ontology-c-with-two-parents)
        (o/with-probe-axioms
          [a (#'o/disjointclasses "a" "b")]
          (-> (#'o/get-current-ontology)
              (.getDisjointClassesAxioms
               (o/owlclass "a"))
              (.size))))))

  ;; add a disjoint test whether it breaks after
  (is
   (= 0
      (do
        (ontology-c-with-two-parents)
        (o/with-probe-axioms
          [a (#'o/disjointclasses "a" "b")])

        (-> (#'o/get-current-ontology)
            (.getDisjointClassesAxioms
             (o/owlclass "a"))
            (.size))))))

(deftest owlimport
  (is
   (not
    (nil?
     (o/owlimport (o/get-current-ontology))))))


(deftest annotation
  (is
   (instance? org.semanticweb.owlapi.model.OWLAnnotation
              (o/label "hello")))

  (is
   (instance? org.semanticweb.owlapi.model.OWLAnnotation
              (o/owlcomment "hello")))

  (is
   (instance? org.semanticweb.owlapi.model.OWLAnnotation
              (o/isdefinedby "hello")))

  (is
   (instance? org.semanticweb.owlapi.model.OWLAnnotation
              (o/seealso "hello")))
  (is
   (instance? org.semanticweb.owlapi.model.OWLAnnotation
              (o/backwardcompatiblewith  "hello")))

  (is
   (instance? org.semanticweb.owlapi.model.OWLAnnotation
              (o/incompatiblewith "hello")))

  (is
   (instance? org.semanticweb.owlapi.model.OWLAnnotationProperty
              (o/annotation-property "hello")))

  (is
   (instance? org.semanticweb.owlapi.model.OWLAnnotation
              (o/annotation
               (o/annotation-property "hello")
               "hello1"))))



(deftest add-annotation
  (is
   (not
    (nil? (#'o/add-annotation
           (o/owlclass "a")
           (list (o/owlcomment "comment"))))))

  (is
   (not
    (nil? (#'o/add-annotation
           testontology
           (list (o/owlcomment "comment")))))))

(deftest add-annotation2
  (is
   (=
    "hello"
    (do
      (let [b (o/owlclass "b")]
        (o/add-annotation
         b (list (o/label "hello")))
        (.getLiteral
         (.getValue
          (first
           (filter
            #(-> %
                 (.getProperty)
                 (.isLabel))
            (.getAnnotations b testontology))))))))))


(deftest dataproperty
  (is (instance? org.semanticweb.owlapi.model.OWLDataProperty 
                 (o/datatypeproperty "hello" :ontology testontology))))



(deftest disjoint?
  (is
   (let [a (o/owlclass "a")
         b (o/owlclass "b")]
     (#'o/disjointclasses a b)
     (o/disjoint? a b))))

;; TODO lots of macros are in serious need of a test

(deftest as-subclasses
  (is
   (let [x (o/owlclass "x")]
     (o/as-subclasses
      x
      (o/owlclass "y")
      (o/owlclass "z"))

     ;; now for the test
     (and (o/superclass?
           (o/owlclass "y")
           x)
          (o/superclass?
           (o/owlclass "z")
           x))))
  (is
   (let [x (o/owlclass "x")]
     (o/as-subclasses
      x :disjoint
      (o/owlclass "y")
      (o/owlclass "z"))
     (o/disjoint?
      (o/owlclass "y")
      (o/owlclass "z"))))

  (is
   (let [x (o/owlclass "x")]
        (o/as-subclasses
         x :cover
         (o/owlclass "y")
         (o/owlclass "z"))

        (o/equivalent?
         (o/owlclass "x")
         (o/owlor (o/owlclass "z")
                  (o/owlclass "y")))))


  (is
   (let [x (o/owlclass "x")]
     (o/as-subclasses
      x :cover
      (o/owlclass "y")
      (o/owlclass "z"))

     (and
      (o/disjoint?
       (o/owlclass "y")
       (o/owlclass "z"))

      (o/equivalent?
       (o/owlclass "x")
       (o/owlor (o/owlclass "z")
                (o/owlclass "y")))))))


(deftest as-disjoint-subclasses
  (is
   (let [x (o/owlclass "x")]
     (o/as-disjoint-subclasses
      x
      (o/owlclass "y")
      (o/owlclass "z"))

     (and
      (o/disjoint?
       (o/owlclass "y")
       (o/owlclass "z"))

      (o/superclass?
       (o/owlclass "y")
       x)

      (o/superclass?
       (o/owlclass "z")
       x)))))

(deftest prefix-suffix-symbol
  (is (= 'helloworld
         (#'o/prefix-symbol "hello" 'world)))
  (is (= 'helloworld
         (#'o/suffix-symbol "world" 'hello))))


(deftest guess
  (is (= :object
         (o/guess-type
          (o/owlclass "a"))))

  (is (= :annotation
         (o/guess-type
          (o/annotation-property "b"))))

  (is (= :object
         (do
           (o/owlclass "c")
           (o/guess-type "c"))))


  (is (= :object
         (o/guess-type
          (list (o/owlclass "d") "e" "f"))))

  (is (= :object
         (o/guess-type
          (list "e" "f" (o/owlclass "d"))))))


(deftest veggiepizza
  (is (= :object
         (do
           (o/guess-type
            (o/with-probe-entities
              [r (o/objectproperty "hasTopping")
               c (o/owlclass "MeatTopping")]
              (o/owlnot
               (o/owlsome r c))))))
      "A regression tester from the pizza ontology"))

(deftest oneof
  (is
   (instance?
    org.semanticweb.owlapi.model.OWLObjectOneOf
    (o/oneof (o/individual "a"))))

  (is 
   (instance?
    org.semanticweb.owlapi.model.OWLDataOneOf
    (o/oneof (o/literal "hello")))))


(deftest add-different
  (is
   (let
       [i1 (o/individual "i1")
        i2 (o/individual "i2")]
     (o/add-different i1 i2)
     (some #{i2}
           (.getDifferentIndividuals i1 testontology)))))


(deftest add-data-super
  (is
   (o/with-probe-entities
     [dp (o/datatypeproperty "a")
      sdp (o/datatypeproperty "b" :subproperty dp)]
     true))
  (is
   (every? #(instance?
             org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom %1)
           (o/with-probe-entities
             [dp (o/datatypeproperty "a")
              sdp (o/datatypeproperty "b")]
             (o/add-data-superproperty
              testontology
              dp sdp)))))
