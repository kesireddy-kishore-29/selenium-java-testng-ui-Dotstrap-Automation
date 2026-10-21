# 4. Video: frames stitched with ffmpeg by default, the Grid's recorder as an option

- Status: accepted
- Date: 2026-10-21

## Context

Selenium has no video recorder. The usual answers are a Selenium Grid in Docker with its video
container, a screen recorder on the machine, or screenshots joined into a video. This repository
was built on a machine where Docker could not run (no WSL), and CI runners vary.

## Decision

1. Default (`VIDEO_MODE=stitch`): a `WebDriverListener` takes a screenshot after every page
   action (navigation, click, typing, resize). After the test, the failure screenshot is added as
   the last frame and ffmpeg joins the frames into `video.webm` (VP9, 2 frames per second), sized
   to the viewport. Works with any local or headless Chrome.
2. Option (`VIDEO_MODE=grid`): tests run on a Selenium Grid; its video container names each video
   after the session, and the evidence step copies it next to the test's evidence (transcoded to
   WebM when ffmpeg is present). `docker/selenium-grid.yml` and the nightly `grid-video` job set
   this up.
3. Without ffmpeg, the frames are kept as numbered screenshots and a note in `meta.json` says why.

## Trade-off

| | Stitched frames | Grid recorder |
|---|---|---|
| Needs | ffmpeg | Docker, a Grid, one session per node per recorder |
| Shows | each page state after an action | real time, including animations and waits |
| A 10 s wait | one frame | 10 s of video |
| Cost | a screenshot per action (slower with `RECORD_MODE=all`) | containers; videos of every session |

## Consequences

- The stitched video is a step-by-step replay, not real time: a long wait is one frame. The page
  source, console and network log fill in what happened between frames.
- The Grid path is written but has **not run** (no Docker on the build machine); see
  [known issues](../known-issues.md).
