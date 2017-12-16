(ns demoworks-exercise.lookup-test
  (:require [demoworks-exercise.lookup :as sut]
            [clojure.test :refer :all]
            [clojure.string :as str]))

(def good-entry (sut/->OCDEntry "123 Place St" "oakland" "ca" "94606" "alameda" nil))

(deftest data-rendering-tests
  (let [expected-country "ocd-division/country:us"
        expected-state   (str expected-country "/state:ca")
        expected-city    (str expected-state "/place:oakland")
        expected-county  (str expected-state "/county:alameda")]
    (testing "Can we correctly format `OCDEntry`s with a single format?"
      (is (= expected-country (sut/render-ocd good-entry [])) "Should get back our country string")
      (is (= expected-state   (sut/render-ocd good-entry sut/state-keys)) "Should get back our state string")
      (is (= expected-city    (sut/render-ocd good-entry sut/city-keys)) "Should get back our city string")
      (is (= expected-county  (sut/render-ocd good-entry sut/county-keys))) "Should get back our county string")

    (testing "Can we parse one OCDEntry into a correct, single string?"
      (let [expected-single-string (str expected-state "," expected-city "," expected-county)]
        (is (= expected-single-string (sut/render-ocds good-entry)) "Is our rendered full string as we'd expect?")))))
