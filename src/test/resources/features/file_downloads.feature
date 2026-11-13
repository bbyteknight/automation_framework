@web @downloads @smoke
Feature: File downloads on Grid and cloud

  Downloads a real file from the-internet's /download page and retrieves it the right way for
  wherever the session actually ran: pulled from the Grid/cloud node via Selenium's
  se:downloadsEnabled API (utils.DownloadUtil) when executionMode=grid|cloud, or read straight
  off disk when local — the same Then steps pass either way.

  Scenario: A downloaded file is retrievable and non-empty regardless of where the session ran
    Given the user is on the file download page
    When the user downloads the first available file
    Then the downloaded file should exist
    And the downloaded file should not be empty
