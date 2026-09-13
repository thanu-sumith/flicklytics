# Flicklytics

A reactive movie and TV analytics application built as a five-person team project for SOEN 6441 at Concordia University. This is a clean public snapshot of the team application, maintained by Thanugundla Sumith Reddy. It does not include the original private repository's history. Existing source attribution is retained; this is not presented as an individually authored application.

## Features

- Live movie, TV and person search using TMDb and WebSockets.
- Financial performance: budget, revenue and ROI analysis.
- Review sentiment classification and Flesch-Kincaid readability scoring.
- Translation/localization metrics and person career statistics.

## Sumith's contributions

- Financial Performance module with asynchronous Pekko actor calculations and supervisor-based recovery from API failures.
- Majority of the HTML/CSS frontend for search results and analytics views.

## Technology

Java, Play Framework 3.0.10, Scala templates, Pekko actors, Guice, Jackson, TMDb, JUnit, Mockito and JaCoCo. The repository pins sbt in `project/build.properties`.

## Run locally

Install a supported JDK and sbt. Set `TMDB_API_KEY` in your local shell or secret manager, then run:

```sh
sbt run
```

Open http://localhost:9000. Never put your real API key in a tracked file. Tests and packaging:

```sh
sbt test
sbt stage
```

## Production hosting

This application needs a running Java server and WebSocket support. It is not a static GitHub Pages application.

Configure these private environment variables on your hosting service:

| Variable | Purpose |
| --- | --- |
| `TMDB_API_KEY` | A new, valid TMDb API key |
| `APPLICATION_SECRET` | A strong, randomly generated Play signing secret |
| `APP_HOST` | Your public hostname only, without protocol or path |

Build with `sbt stage`. Start the executable generated in `target/universal/stage/bin/`, supplying the host's port through the Play `-Dhttp.port` setting. Terminate HTTPS at the hosting service's proxy. Browser WebSockets automatically use WSS on HTTPS pages.

No live deployment is claimed by this repository. Hosting credentials and a replacement API key are required before launch. Revoke any previously committed API key before using this application publicly.

## Data and limitations

This product uses the TMDb API but is not endorsed or certified by TMDb. Analytics depend on available upstream data; sentiment classifications are heuristic application outputs, not ground truth.

## Attribution and permissions

This is a university team project. Source authorship comments are preserved. No new license is applied to other contributors' work. Obtain the appropriate permissions before reuse beyond viewing this portfolio example.
