(ns puppetlabs.puppetdb.import
  "Import utility

   This is a command-line tool for importing data into PuppetDB. It expects
   as input a tarball generated by the PuppetDB `export` command-line tool."
  (:import [org.apache.commons.compress.archivers.tar TarArchiveEntry]
           [puppetlabs.puppetdb.archive TarGzReader]
           [java.io File Closeable])
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [puppetlabs.puppetdb.archive :as archive]
            [puppetlabs.puppetdb.reports :as reports]
            [puppetlabs.puppetdb.export :as export]
            [puppetlabs.puppetdb.utils :as utils]
            [puppetlabs.puppetdb.cheshire :as json]
            [puppetlabs.puppetdb.schema :refer [defn-validated]]
            [puppetlabs.puppetdb.utils :as utils]
            [puppetlabs.i18n.core :refer [trs tru]]))

(defn make-certname-matcher
  "Returns a function that find a certname in the `file-path` for the
  given `entity`, the function returns nil if the path is not
  recognized"
  [entity with-hash?]
  (let [command-certname-regex (re-pattern
                                (str "^"
                                     (apply str
                                            (map utils/regex-quote
                                                 [utils/export-root-dir
                                                  File/separatorChar
                                                  entity
                                                  File/separatorChar]))

                                     "(.*)"
                                     (when with-hash?
                                       "\\-\\p{XDigit}{40}")
                                     "\\.json$"))]
    (fn [file-path]
      (when-let [[_ certname-match] (re-matches command-certname-regex file-path)]
        [entity certname-match]))))

(def command-matcher
  (some-fn (make-certname-matcher "catalogs" true)
           (make-certname-matcher "reports" true)
           (make-certname-matcher "facts" false)
           (make-certname-matcher "configure_expiration" true)
           (make-certname-matcher "catalog_inputs" true)))

(defn-validated process-tar-entry
  "Determine the type of an entry from the exported archive, and process it
  accordingly."
  [command-fn
   tar-reader :- TarGzReader
   tar-entry :- TarArchiveEntry
   command-versions]
  (let [path (.getName tar-entry)
        [command-type certname] (command-matcher path)
        compression ""
        command-fn' (fn [command-kwd command-version]
                      (command-fn command-kwd
                                  command-version
                                  certname
                                  nil ;-> No producer timestamp on imports
                                  (-> tar-reader
                                      utils/read-json-content
                                      json/generate-string
                                      (.getBytes "UTF-8")
                                      java.io.ByteArrayInputStream.)
                                  compression))]
    (case command-type
      "catalogs"
      (do
        (log/info (trs "Importing catalog from archive entry ''{0}''" path))
        (command-fn' :replace-catalog
                     (:replace_catalog command-versions)))
      "reports"
      (do
        (log/info (trs "Importing report from archive entry ''{0}''" path))
        (command-fn' :store-report
                    (:store_report command-versions)))
      "facts"
      (do
        (log/info (trs "Importing facts from archive entry ''{0}''" path))
        (command-fn' :replace-facts
                     (:replace_facts command-versions)))
      "configure_expiration"
      (do
        (log/info (trs "Importing node expiration from archive entry ''{0}''" path))
        (command-fn' :configure-expiration
                     (:configure_expiration command-versions)))
      "catalog_inputs"
      (do
        (log/info (trs "Importing catalog input from archive entry ''{0}''" path))
        (command-fn' :replace-catalog-inputs
                     (:replace_catalog_inputs command-versions)))
      nil)))

(def metadata-path
  (.getPath (io/file utils/export-root-dir export/export-metadata-file-name)))

(defn parse-metadata
  "Parses the export metadata file to determine, e.g., what versions of the
  commands should be used during import."
  [tar-reader]
  {:post [(map? %)
          (:command_versions %)]}
  (when-not (archive/find-entry tar-reader metadata-path)
    (throw (IllegalStateException.
            (tru "Unable to find export metadata file ''{0}'' in archive" metadata-path))))
  (utils/read-json-content tar-reader true))

(defn import!
  [infile command-fn]
  (with-open [tar-reader (archive/tarball-reader infile)]
    (let [command-versions (:command_versions (parse-metadata tar-reader))]
      (doseq [tar-entry (archive/all-entries tar-reader)]
        (process-tar-entry command-fn tar-reader tar-entry command-versions)))))
