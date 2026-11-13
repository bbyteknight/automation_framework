@web @uploads @smoke
Feature: File uploads on remote sessions

  Uploads a fixture file from resources/testdata/uploads to the-internet's /upload page. The same
  steps pass unchanged whether the browser session is local or running on a remote Grid node —
  DriverFactory registers a LocalFileDetector on every RemoteWebDriver, which ships the local
  file to the node transparently before Selenium types its path into the file input.

  Scenario: An uploaded file is accepted and its name is echoed back
    Given the user is on the file upload page
    When the user uploads the file "sample-upload.txt"
    Then the uploaded file name should be displayed as "sample-upload.txt"
