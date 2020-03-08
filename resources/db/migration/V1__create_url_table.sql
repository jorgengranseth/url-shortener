CREATE TABLE "url" (
    full_url    VARCHAR(255)    NOT NULL,
    short_url   VARCHAR(8)      NOT NULL,
    clicks      INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT "PK_URL_SHORT_URL" PRIMARY KEY (short_url)
);
