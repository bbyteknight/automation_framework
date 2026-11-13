@api @files @smoke
Feature: Zip/Unzip archive validation

  Built from the small fixture directory at resources/files/sample_source (hello.txt, world.txt)
  rather than any real downloaded file — the framework has no browser-download plumbing today, so
  these steps double as the pattern to point at a real downloaded zip's path once one exists.
  Every extraction folder is auto-tracked and deleted after the scenario (steps.FileSteps /
  utils.ZipExtractionManager) regardless of pass/fail.

  Scenario: A created zip archive contains the expected files
    Given a zip archive is created from the sample source directory
    Then the downloaded zip should contain file "hello.txt"
    And the zip archive should contain file "world.txt"
    And the zip archive should contain 2 files

  Scenario: A zip archive can be extracted and its contents validated on disk
    Given a zip archive is created from the sample source directory
    When the zip archive is extracted
    Then the extracted files should contain "hello.txt"
    And the extracted files should contain "world.txt"

  Scenario: A password-protected AES zip archive can be created and extracted
    Given a password-protected zip archive is created from the sample source directory with password "Secret123!"
    Then the zip archive should contain file "hello.txt"
    When the zip archive is extracted with password "Secret123!"
    Then the extracted files should contain "hello.txt"
