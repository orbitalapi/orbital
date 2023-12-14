import {SearchResult} from "../search/search.service";

export const searchResults: SearchResult[] = [{
  "qualifiedName": {
    "fullyQualifiedName": "film.Film",
    "parameters": [],
    "name": "Film",
    "parameterizedName": "film.Film",
    "namespace": "film",
    "longDisplayName": "film.Film",
    "shortDisplayName": "Film"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {"field": "NAME", "highlightedMatch": "<span class='matchedText'>Film</span>"}, {
    "field": "NAME_AS_WORDS",
    "highlightedMatch": "<span class='matchedText'>Film</span>"
  }, {"field": "QUALIFIED_NAME", "highlightedMatch": "<span class='matchedText'>film</span>.Film"}, {
    "field": "NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>_id"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.Film"
  }, {"field": "QUALIFIED_NAME", "highlightedMatch": "<span class='matchedText'>film</span>.Film"}],
  "memberType": "TYPE",
  "score": 5.636926,
  "consumers": [],
  "producers": [],
  "metadata": [{
    "name": {
      "fullyQualifiedName": "io.vyne.jdbc.Table",
      "parameters": [],
      "name": "Table",
      "parameterizedName": "io.vyne.jdbc.Table",
      "namespace": "io.vyne.jdbc",
      "longDisplayName": "io.vyne.jdbc.Table",
      "shortDisplayName": "Table"
    }, "params": {"table": "film", "schema": "public", "connection": "films"}
  }],
  "typeKind": "Model",
  "serviceKind": null,
  "primitiveType": null
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.FilmId",
    "parameters": [],
    "name": "FilmId",
    "parameterizedName": "film.types.FilmId",
    "namespace": "film.types",
    "longDisplayName": "film.types.FilmId",
    "shortDisplayName": "FilmId"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.types.FilmId"
  }, {"field": "NAME", "highlightedMatch": "<span class='matchedText'>Film</span>Id"}],
  "memberType": "TYPE",
  "score": 4.781121,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Int",
    "parameters": [],
    "name": "Int",
    "parameterizedName": "lang.taxi.Int",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Int",
    "shortDisplayName": "Int"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "films.FilmId",
    "parameters": [],
    "name": "FilmId",
    "parameterizedName": "films.FilmId",
    "namespace": "films",
    "longDisplayName": "films.FilmId",
    "shortDisplayName": "FilmId"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>s.FilmId"
  }, {"field": "NAME", "highlightedMatch": "<span class='matchedText'>Film</span>Id"}],
  "memberType": "TYPE",
  "score": 4.781121,
  "consumers": [{
    "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
    "parameters": [],
    "name": "IdLookupService@@lookupFromInternalFilmId",
    "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
    "namespace": "io.vyne.films.idlookup",
    "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
    "shortDisplayName": "lookupFromInternalFilmId"
  }, {
    "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
    "parameters": [],
    "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
    "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
    "namespace": "io.vyne.demos.films",
    "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
    "shortDisplayName": "getStreamingProvidersForFilm"
  }],
  "producers": [{
    "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
    "parameters": [],
    "name": "IdLookupService@@lookupFromSquashedTomatoesId",
    "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
    "namespace": "io.vyne.films.idlookup",
    "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
    "shortDisplayName": "lookupFromSquashedTomatoesId"
  }, {
    "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
    "parameters": [],
    "name": "IdLookupService@@lookupFromNetflixFilmId",
    "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
    "namespace": "io.vyne.films.idlookup",
    "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
    "shortDisplayName": "lookupFromNetflixFilmId"
  }],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Int",
    "parameters": [],
    "name": "Int",
    "parameterizedName": "lang.taxi.Int",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Int",
    "shortDisplayName": "Int"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "films.reviews.FilmReviewScore",
    "parameters": [],
    "name": "FilmReviewScore",
    "parameterizedName": "films.reviews.FilmReviewScore",
    "namespace": "films.reviews",
    "longDisplayName": "films.reviews.FilmReviewScore",
    "shortDisplayName": "FilmReviewScore"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>s.reviews.FilmReviewScore"
  }, {"field": "NAME", "highlightedMatch": "<span class='matchedText'>Film</span>ReviewScore"}],
  "memberType": "TYPE",
  "score": 4.5,
  "consumers": [],
  "producers": [{
    "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
    "parameters": [],
    "name": "ReviewsService@@getReview",
    "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
    "namespace": "io.vyne.reviews",
    "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
    "shortDisplayName": "getReview"
  }],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Decimal",
    "parameters": [],
    "name": "Decimal",
    "parameterizedName": "lang.taxi.Decimal",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Decimal",
    "shortDisplayName": "Decimal"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.FilmDatabase",
    "parameters": [],
    "name": "FilmDatabase",
    "parameterizedName": "film.FilmDatabase",
    "namespace": "film",
    "longDisplayName": "film.FilmDatabase",
    "shortDisplayName": "FilmDatabase"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.FilmDatabase"
  }, {"field": "NAME", "highlightedMatch": "<span class='matchedText'>Film</span>Database"}],
  "memberType": "SERVICE",
  "score": 4.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": null,
  "serviceKind": "Database",
  "primitiveType": null
}, {
  "qualifiedName": {
    "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
    "parameters": [],
    "name": "IdResolution",
    "parameterizedName": "io.vyne.films.idlookup.IdResolution",
    "namespace": "io.vyne.films.idlookup",
    "longDisplayName": "io.vyne.films.idlookup.IdResolution",
    "shortDisplayName": "IdResolution"
  },
  "typeDoc": null,
  "matchedFieldName": "filmId",
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "io.vyne.<span class='matchedText'>film</span>s.idlookup.IdResolution"
  }, {"field": "NAME", "highlightedMatch": "<span class='matchedText'>film</span>Id"}],
  "memberType": "ATTRIBUTE",
  "score": 3.2811208,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Int",
    "parameters": [],
    "name": "Int",
    "parameterizedName": "lang.taxi.Int",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Int",
    "shortDisplayName": "Int"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "NewFilmReleaseAnnouncement",
    "parameters": [],
    "name": "NewFilmReleaseAnnouncement",
    "parameterizedName": "NewFilmReleaseAnnouncement",
    "namespace": "",
    "longDisplayName": "NewFilmReleaseAnnouncement",
    "shortDisplayName": "NewFilmReleaseAnnouncement"
  },
  "typeDoc": null,
  "matchedFieldName": "filmId",
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "New<span class='matchedText'>Film</span>ReleaseAnnouncement"
  }, {"field": "NAME", "highlightedMatch": "<span class='matchedText'>film</span>Id"}],
  "memberType": "ATTRIBUTE",
  "score": 3.2811208,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Int",
    "parameters": [],
    "name": "Int",
    "parameterizedName": "lang.taxi.Int",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Int",
    "shortDisplayName": "Int"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "io.vyne.reviews.FilmReview",
    "parameters": [],
    "name": "FilmReview",
    "parameterizedName": "io.vyne.reviews.FilmReview",
    "namespace": "io.vyne.reviews",
    "longDisplayName": "io.vyne.reviews.FilmReview",
    "shortDisplayName": "FilmReview"
  },
  "typeDoc": null,
  "matchedFieldName": "filmId",
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "io.vyne.reviews.<span class='matchedText'>Film</span>Review"
  }, {"field": "NAME", "highlightedMatch": "<span class='matchedText'>film</span>Id"}, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "io.vyne.reviews.<span class='matchedText'>Film</span>Review"
  }, {"field": "NAME", "highlightedMatch": "<span class='matchedText'>Film</span>Review"}, {
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "io.vyne.reviews.<span class='matchedText'>Film</span>Review"
  }, {"field": "NAME", "highlightedMatch": "<span class='matchedText'>film</span>Review"}],
  "memberType": "ATTRIBUTE",
  "score": 3.2811208,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.String",
    "parameters": [],
    "name": "String",
    "parameterizedName": "lang.taxi.String",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.String",
    "shortDisplayName": "String"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.ReplacementCost",
    "parameters": [],
    "name": "ReplacementCost",
    "parameterizedName": "film.types.ReplacementCost",
    "namespace": "film.types",
    "longDisplayName": "film.types.ReplacementCost",
    "shortDisplayName": "ReplacementCost"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.types.ReplacementCost"
  }],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Decimal",
    "parameters": [],
    "name": "Decimal",
    "parameterizedName": "lang.taxi.Decimal",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Decimal",
    "shortDisplayName": "Decimal"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.LastUpdate",
    "parameters": [],
    "name": "LastUpdate",
    "parameterizedName": "film.types.LastUpdate",
    "namespace": "film.types",
    "longDisplayName": "film.types.LastUpdate",
    "shortDisplayName": "LastUpdate"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.types.LastUpdate"
  }],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Instant",
    "parameters": [],
    "name": "Instant",
    "parameterizedName": "lang.taxi.Instant",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Instant",
    "shortDisplayName": "Instant"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.Title",
    "parameters": [],
    "name": "Title",
    "parameterizedName": "film.types.Title",
    "namespace": "film.types",
    "longDisplayName": "film.types.Title",
    "shortDisplayName": "Title"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{"field": "QUALIFIED_NAME", "highlightedMatch": "<span class='matchedText'>film</span>.types.Title"}],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.String",
    "parameters": [],
    "name": "String",
    "parameterizedName": "lang.taxi.String",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.String",
    "shortDisplayName": "String"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.Fulltext",
    "parameters": [],
    "name": "Fulltext",
    "parameterizedName": "film.types.Fulltext",
    "namespace": "film.types",
    "longDisplayName": "film.types.Fulltext",
    "shortDisplayName": "Fulltext"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{"field": "QUALIFIED_NAME", "highlightedMatch": "<span class='matchedText'>film</span>.types.Fulltext"}],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Any",
    "parameters": [],
    "name": "Any",
    "parameterizedName": "lang.taxi.Any",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Any",
    "shortDisplayName": "Any"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.Description",
    "parameters": [],
    "name": "Description",
    "parameterizedName": "film.types.Description",
    "namespace": "film.types",
    "longDisplayName": "film.types.Description",
    "shortDisplayName": "Description"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.types.Description"
  }],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.String",
    "parameters": [],
    "name": "String",
    "parameterizedName": "lang.taxi.String",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.String",
    "shortDisplayName": "String"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.ReleaseYear",
    "parameters": [],
    "name": "ReleaseYear",
    "parameterizedName": "film.types.ReleaseYear",
    "namespace": "film.types",
    "longDisplayName": "film.types.ReleaseYear",
    "shortDisplayName": "ReleaseYear"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.types.ReleaseYear"
  }],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Any",
    "parameters": [],
    "name": "Any",
    "parameterizedName": "lang.taxi.Any",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Any",
    "shortDisplayName": "Any"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.SpecialFeatures",
    "parameters": [],
    "name": "SpecialFeatures",
    "parameterizedName": "film.types.SpecialFeatures",
    "namespace": "film.types",
    "longDisplayName": "film.types.SpecialFeatures",
    "shortDisplayName": "SpecialFeatures"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.types.SpecialFeatures"
  }],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.String",
    "parameters": [],
    "name": "String",
    "parameterizedName": "lang.taxi.String",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.String",
    "shortDisplayName": "String"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.Length",
    "parameters": [],
    "name": "Length",
    "parameterizedName": "film.types.Length",
    "namespace": "film.types",
    "longDisplayName": "film.types.Length",
    "shortDisplayName": "Length"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{"field": "QUALIFIED_NAME", "highlightedMatch": "<span class='matchedText'>film</span>.types.Length"}],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Int",
    "parameters": [],
    "name": "Int",
    "parameterizedName": "lang.taxi.Int",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Int",
    "shortDisplayName": "Int"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.RentalRate",
    "parameters": [],
    "name": "RentalRate",
    "parameterizedName": "film.types.RentalRate",
    "namespace": "film.types",
    "longDisplayName": "film.types.RentalRate",
    "shortDisplayName": "RentalRate"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.types.RentalRate"
  }],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Decimal",
    "parameters": [],
    "name": "Decimal",
    "parameterizedName": "lang.taxi.Decimal",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Decimal",
    "shortDisplayName": "Decimal"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.Rating",
    "parameters": [],
    "name": "Rating",
    "parameterizedName": "film.types.Rating",
    "namespace": "film.types",
    "longDisplayName": "film.types.Rating",
    "shortDisplayName": "Rating"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{"field": "QUALIFIED_NAME", "highlightedMatch": "<span class='matchedText'>film</span>.types.Rating"}],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.String",
    "parameters": [],
    "name": "String",
    "parameterizedName": "lang.taxi.String",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.String",
    "shortDisplayName": "String"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "film.types.RentalDuration",
    "parameters": [],
    "name": "RentalDuration",
    "parameterizedName": "film.types.RentalDuration",
    "namespace": "film.types",
    "longDisplayName": "film.types.RentalDuration",
    "shortDisplayName": "RentalDuration"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>.types.RentalDuration"
  }],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Int",
    "parameters": [],
    "name": "Int",
    "parameterizedName": "lang.taxi.Int",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Int",
    "shortDisplayName": "Int"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
    "parameters": [],
    "name": "SquashedTomatoesFilmId",
    "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
    "namespace": "films.reviews",
    "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
    "shortDisplayName": "SquashedTomatoesFilmId"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>s.reviews.SquashedTomatoesFilmId"
  }, {"field": "NAME", "highlightedMatch": "SquashedTomatoes<span class='matchedText'>Film</span>Id"}],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [{
    "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
    "parameters": [],
    "name": "IdLookupService@@lookupFromSquashedTomatoesId",
    "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
    "namespace": "io.vyne.films.idlookup",
    "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
    "shortDisplayName": "lookupFromSquashedTomatoesId"
  }, {
    "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
    "parameters": [],
    "name": "ReviewsService@@getReview",
    "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
    "namespace": "io.vyne.reviews",
    "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
    "shortDisplayName": "getReview"
  }],
  "producers": [{
    "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
    "parameters": [],
    "name": "IdLookupService@@lookupFromInternalFilmId",
    "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
    "namespace": "io.vyne.films.idlookup",
    "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
    "shortDisplayName": "lookupFromInternalFilmId"
  }, {
    "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
    "parameters": [],
    "name": "IdLookupService@@lookupFromNetflixFilmId",
    "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
    "namespace": "io.vyne.films.idlookup",
    "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
    "shortDisplayName": "lookupFromNetflixFilmId"
  }],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.String",
    "parameters": [],
    "name": "String",
    "parameterizedName": "lang.taxi.String",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.String",
    "shortDisplayName": "String"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "films.reviews.ReviewText",
    "parameters": [],
    "name": "ReviewText",
    "parameterizedName": "films.reviews.ReviewText",
    "namespace": "films.reviews",
    "longDisplayName": "films.reviews.ReviewText",
    "shortDisplayName": "ReviewText"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>s.reviews.ReviewText"
  }],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [{
    "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
    "parameters": [],
    "name": "ReviewsService@@getReview",
    "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
    "namespace": "io.vyne.reviews",
    "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
    "shortDisplayName": "getReview"
  }],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.String",
    "parameters": [],
    "name": "String",
    "parameterizedName": "lang.taxi.String",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.String",
    "shortDisplayName": "String"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "films.StreamingProviderName",
    "parameters": [],
    "name": "StreamingProviderName",
    "parameterizedName": "films.StreamingProviderName",
    "namespace": "films",
    "longDisplayName": "films.StreamingProviderName",
    "shortDisplayName": "StreamingProviderName"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>s.StreamingProviderName"
  }],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [{
    "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
    "parameters": [],
    "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
    "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
    "namespace": "io.vyne.demos.films",
    "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
    "shortDisplayName": "getStreamingProvidersForFilm"
  }],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.String",
    "parameters": [],
    "name": "String",
    "parameterizedName": "lang.taxi.String",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.String",
    "shortDisplayName": "String"
  }
}, {
  "qualifiedName": {
    "fullyQualifiedName": "films.StreamingProviderPrice",
    "parameters": [],
    "name": "StreamingProviderPrice",
    "parameterizedName": "films.StreamingProviderPrice",
    "namespace": "films",
    "longDisplayName": "films.StreamingProviderPrice",
    "shortDisplayName": "StreamingProviderPrice"
  },
  "typeDoc": null,
  "matchedFieldName": null,
  "matches": [{
    "field": "QUALIFIED_NAME",
    "highlightedMatch": "<span class='matchedText'>film</span>s.StreamingProviderPrice"
  }],
  "memberType": "TYPE",
  "score": 1.5,
  "consumers": [],
  "producers": [{
    "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
    "parameters": [],
    "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
    "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
    "namespace": "io.vyne.demos.films",
    "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
    "shortDisplayName": "getStreamingProvidersForFilm"
  }],
  "metadata": [],
  "typeKind": "Type",
  "serviceKind": null,
  "primitiveType": {
    "fullyQualifiedName": "lang.taxi.Decimal",
    "parameters": [],
    "name": "Decimal",
    "parameterizedName": "lang.taxi.Decimal",
    "namespace": "lang.taxi",
    "longDisplayName": "lang.taxi.Decimal",
    "shortDisplayName": "Decimal"
  }
}] as any as SearchResult[]
