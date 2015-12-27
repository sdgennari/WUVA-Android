# WUVA Radio App #

Website: [Live Radio and Song History](http://player.listenlive.co/46461)

## Triton Digital ##

**[SDK Reference PDF](http://triton-sdk.media.streamtheworld.com/mobile_sdk/TD-Mobile-Android-SDK-2.2.5.pdf)**

**[SDK Class Documentation](http://triton-sdk.media.streamtheworld.com/mobile_sdk/android_api_reference/html/annotated.html)**

**Permissions (from SDK Reference PDF):**

| Permission           | Player | Ads |
| -------------------- |:------:|:---:|
| INTERNET             | Y      | Y   |
| ACCESS_NETWORK_STATE | Y      | Y   |
| WAKE_LOCK            | Y      | N   |

**Important Methods:**

Retrieving metadata from streams

```java
<Activity> implements MediaPlayer.OnCuePointReceivedListener {

    /**
     * Used to retrieve metadata from stream
     * cue_title = the name of the track
     * track_artist_name = name of the artist
     */
    @Override
    public void onCuePointReceived(MediaPlayer player, Bundle cuePoint) {
        ...
    }

}
```

**Important Files from the SDK Sample:**

* StationPlayerActivity.java
* TritonPlayerActivity.java

## ![Cover Art Archive](http://coverartarchive.org/img/navbar_logo.svg) ##

**[API reference](https://musicbrainz.org/doc/Cover_Art_Archive/API)**

Used to get album art for tracks playing on the live stream

**Steps:**

1. Get the `cue_title` and `track_artist_name` from the Triton Station Player
2. Use `cue_title` and `track_artist_name` to query [MusicBrainz Search API](http://musicbrainz.org/doc/Development/XML_Web_Service/Version_2/Search) for the 36 character `MBID` ([info](https://musicbrainz.org/doc/MusicBrainz_Identifier))
    * Search for a `recording` with the title and artist
    * Url encode the parameters with the following syntax `<cue_title> AND artist:"<track_artist_name>"&limit=1&fmt=json`
    * Retrieve the `id` from the first element in the `releases` array
    	* If no `id` exists, a placeholder image should be displayed
    * Sample query:
    	* `cue_title`: Talladega
    	* `track_artist_name`: Eric Church
    	* Search url: [http://musicbrainz.org/ws/2/recording/?query=talladega%20AND%20artist:%22eric%20church%22&limit=1&fmt=json](http://musicbrainz.org/ws/2/recording/?query=talladega%20AND%20artist:%22eric%20church%22&limit=1&fmt=json)
    	* Resulting id: `886eb853-44be-46ef-a99f-4c61bf3c404a`
3. Use the `id` to construct an image url based on the the [Cover Art Archive API](https://musicbrainz.org/doc/Cover_Art_Archive/API)
	* Url syntax: [http://coverartarchive.org/release/id/front-500]()
	* Sample image url: *[http://coverartarchive.org/release/886eb853-44be-46ef-a99f-4c61bf3c404a/front-500](http://coverartarchive.org/release/886eb853-44be-46ef-a99f-4c61bf3c404a/front-500)*
4. Load and display the image with [Picasso](http://square.github.io/picasso/)
	* If no image exists, Picasso will generate an error and a placeholder image should be displayed