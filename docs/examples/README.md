# Examples from real runs

Everything here was produced by a run on the build machine (Windows 11, headless Chrome 154); none
of it is mocked up. Live-site evidence is never kept here.

| Folder | What | From run |
|---|---|---|
| [allure/](allure/) | `overview.png`: the Allure report of a regression run with the two-run trend, environment and feature labels (the README's hero). `categories.png`: the deliberate failures sorted into `LOCATOR_DRIFT`, `PRODUCT_BUG` and `TIMING` | `sel-allure-2` (20/20 passed, after `sel-allure-1`), `sel-demo-3` |
| [gallery/](gallery/) | `gallery.png`: the run gallery (the family's shared page) for the deliberate failures, each attempt with its video and evidence links | `sel-demo-4` |
| [failure-evidence/](failure-evidence/) | A complete failure bundle: two attempts, each with video, screenshots, page source, network log and metadata | `sel-demo-5`, git `aa8ee491f378` |

Commands:

```bash
rm -rf target/allure-results
RUN_ID=sel-allure-1 ./mvnw test -Dsuite=regression && scripts/allure-report.sh
RUN_ID=sel-allure-2 ./mvnw test -Dsuite=regression && scripts/allure-report.sh <copy of report 1>
RUN_ID=sel-demo-4 ./mvnw test -Dsuite=demo-failure      # fails on purpose (the gallery)
RUN_ID=sel-demo-5 ./mvnw test -Dsuite=demo-failure      # again, after the package rename (the bundle)
```
