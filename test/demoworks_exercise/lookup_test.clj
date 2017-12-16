(ns demoworks-exercise.lookup-test
  (:require [demoworks-exercise.lookup :as sut]
            [clojure.test :refer :all]))

(def good-entry (sut/->OCDEntry "123 Place St" "oakland" "ca" "94606" "alameda" nil))

(deftest data-rendering-tests
  (testing "Can we correctly format `OCDEntry`s?"
    (let [expected-country "ocd-division/country:us"
          expected-state   (str expected-country "/state:ca")
          expected-city    (str expected-state "/place:oakland")
          expected-county  (str expected-state "/county:alameda")]
      (is (= expected-country (sut/render-ocd good-entry [])) "Should get back our country string")
      (is (= expected-state   (sut/render-ocd good-entry sut/state-keys)) "Should get back our state string")
      (is (= expected-city    (sut/render-ocd good-entry sut/city-keys)) "Should get back our city string")
      (is (= expected-county  (sut/render-ocd good-entry sut/county-keys))) "Should get back our county string")))
