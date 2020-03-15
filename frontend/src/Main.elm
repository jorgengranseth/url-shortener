module Main exposing (main)

import Browser
import Browser.Navigation
import Html exposing (..)
import Html.Attributes exposing (for, href, id, placeholder, type_, value)
import Html.Events exposing (onClick, onInput, onSubmit)
import Http
import Json.Decode exposing (Decoder)
import Json.Encode


type alias Model =
    { input : String
    , urls : Result Http.Error (List RepositoryUrl)
    }


main : Program () Model Msg
main =
    Browser.element
        { init = always init
        , update = update
        , view = view
        , subscriptions = always Sub.none
        }


init : ( Model, Cmd Msg )
init =
    ( { input = "", urls = Ok [] }
    , Http.get
        { url = "http://localhost:8080/urls"
        , expect = Http.expectJson ReceivedUrls (Json.Decode.list repoUrlDecoder)
        }
    )


repoUrlDecoder : Decoder RepositoryUrl
repoUrlDecoder =
    Json.Decode.map3 RepositoryUrl
        (Json.Decode.field "fullUrl" Json.Decode.string)
        (Json.Decode.field "shortUrl" Json.Decode.string)
        (Json.Decode.field "clicks" Json.Decode.int)


type alias RepositoryUrl =
    { fullUrl : String, shortUrl : String, clicks : Int }


type Msg
    = SubmitUrl
    | InputUrl String
    | UrlSubmitted (Result Http.Error RepositoryUrl)
    | ReceivedUrls (Result Http.Error (List RepositoryUrl))
    | DeleteUrl String
    | UrlDeleted


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        SubmitUrl ->
            ( model
            , Http.post
                { url = "/urls"
                , body = Http.jsonBody <| Json.Encode.object [ ( "fullUrl", Json.Encode.string model.input ) ]
                , expect = Http.expectJson UrlSubmitted repoUrlDecoder
                }
            )

        DeleteUrl fullUrl ->
            ( model
            , Http.request
                { method = "DELETE"
                , headers = []
                , url = "/urls"
                , body = Http.jsonBody <| Json.Encode.object [ ( "fullUrl", Json.Encode.string fullUrl ) ]
                , expect = Http.expectWhatever (always UrlDeleted)
                , timeout = Nothing
                , tracker = Nothing
                }
            )

        InputUrl url ->
            ( { model | input = url }, Cmd.none )

        ReceivedUrls result ->
            ( { model | urls = result }, Cmd.none )

        UrlSubmitted response ->
            let
                newUrls =
                    case response of
                        Ok value ->
                            model.urls
                                |> Result.map (\urls -> urls ++ List.singleton value)
                                |> Result.withDefault (List.singleton value)
                                |> Ok

                        Err error ->
                            model.urls
            in
            ( { model | urls = newUrls }, Cmd.none )

        UrlDeleted ->
            ( model, Browser.Navigation.reloadAndSkipCache )


view : Model -> Html Msg
view model =
    div []
        [ h1 [] [ Html.text "URL Shortener" ]
        , case model.urls of
            Ok yay ->
                viewYay yay

            Err err ->
                div [] [ "Oh no" |> text ]
        , label [ for "urlInput" ] [ text "Submit url" ]
        , input [ id "urlInput", value model.input, type_ "url", placeholder "Submit url", onInput InputUrl ] []
        , button [ onClick SubmitUrl ] [ "Submit URL" |> text ]
        ]


viewYay : List RepositoryUrl -> Html Msg
viewYay urls =
    table [] <|
        [ thead [] [ th [] [ "Short" |> text ], th [] [ "Destination" |> text ], th [] [ "Clicks" |> text ] ]
        ]
            ++ List.map viewUrl urls


viewUrl : RepositoryUrl -> Html Msg
viewUrl repositoryUrl =
    tr []
        [ td [] [ a [ href ("/" ++ repositoryUrl.shortUrl) ] [ repositoryUrl.shortUrl |> text ] ]
        , td [] [ repositoryUrl.fullUrl |> text ]
        , td [] [ repositoryUrl.clicks |> String.fromInt |> text ]
        , td [] [ button [ onClick (DeleteUrl repositoryUrl.fullUrl) ] [ text "Delete URL" ] ]
        ]
