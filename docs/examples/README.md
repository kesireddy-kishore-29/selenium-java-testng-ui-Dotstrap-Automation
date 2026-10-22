# Examples from real runs

Everything here was produced by a run on the build machine (Windows 11, headless Chrome 154); none
of it is mocked up. Live-site evidence is never kept here.

| Folder | What | From run |
|---|---|---|
| [allure/](allure/) | `overview.png`: the Allure report of a regression run with the two-run trend, environment and feature labels (the README's hero). `categories.png`: the deliberate failures sorted into `LOCATOR_DRIFT`, `PRODUCT_BUG` and `TIMING` | `sel-allure-4` (20/20 passed, after `sel-allure-3`), `sel-demo-7` |
| [gallery/](gallery/) | `gallery.png`: the run gallery (the family's shared page) for the deliberate failures, each attempt with its video and evidence links | `sel-demo-7` |
| [failure-evidence/](failure-evidence/) | A complete failure bundle: two attempts, each with video, screenshots, page source, network log and metadata | `sel-demo-7`, git `7cf5ac6158be` |

Commands:

```bash
# ffmpeg on PATH (the videos are stitched with it)
rm -rf target/allure-results
RUN_ID=sel-allure-3 ./mvnw test -Dsuite=regression && scripts/allure-report.sh
cp -r target/site/allure-maven-plugin /tmp/report-1 && rm -rf target/allure-results
RUN_ID=sel-allure-4 ./mvnw test -Dsuite=regression && scripts/allure-report.sh /tmp/report-1
rm -rf target/allure-results
RUN_ID=sel-demo-7 ./mvnw test -Dsuite=demo-failure      # fails on purpose (categories, gallery, bundle)
scripts/allure-report.sh
```
