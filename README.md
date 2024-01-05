"Lambdagoyf" is a hobby project that attempts to solve several problems related to data about the card game _Magic: The Gathering_.

Features
========

This project's main purposes are:

1. to ingest card data from Scryfall, a third-party resource site;
2. to provide a clear, specific, object-oriented model of _Magic_ cards at varying levels of specificity; and
3. to offer a feature-rich, highly customizable tool for generating importable deck files for _Magic Online_ and _Magic Arena_.

Scryfall ingestion
------------------

[Scryfall][scryfall] is an amazing project, a labor of love, and a saving grace for a hobby community that's otherwise plagued by data that's often unclear, buggy, or out of date. It offers [an API][scryfall-api], but you don't generally need the API if you download the [bulk JSON data][scryfall-bulk-data] instead.

  [scryfall]:           https://scryfall.com/
  [scryfall-api]:       https://scryfall.com/docs/api
  [scryfall-bulk-data]: https://scryfall.com/docs/api/bulk-data

It's worth mentioning that Scryfall's data, and the way it models its data, is the foundation on which the rest of this project is built. Ingesting that data is also the logical first step in any workflow that this program can execute.

The program expects a download of Scryfall's bulk JSON data to be readable from the local disk. The `ScryfallFetcher` class automatically downloads and refreshes those files from the Scryfall API, ensuring that it does so only at certain intervals so that you don't inadvertently hammer their servers.

Please note that nothing in the following section should be interpreted as a critique of Scryfall's own data model. Scryfall's model makes perfect sense for what it is: a JSON-based API. My own work is meant to build on top of it (standing on the shoulders of Hill Giants and all) and adapt it into a narrower, more Java-centric form suitable for writing application code.

Card modeling
-------------

### Motivation

Software related to _Magic: The Gathering_, in my opinion, suffers from an endemic data-modeling problem. _Magic_ cards are complicated, and they seem to get more complicated every passing year. Attempts to represent them in software seem to be chronically prone to semantic snags. To examine this, kindly indulge some rhetorical questions.

When is it correct to say that two cards are the same card? Is a _Revised Edition_ Lightning Bolt the same as a _Fourth Edition_ Lightning Bolt? The answer must be contextual: the answer is "yes" if you're talking about the composition of a deck in a tournament but "no" if you're selling singles from an online storefront.

What counts as one card? Consider a double-faced card, or a split card such as Fire/Ice. Is Fire/Ice one card or two? In a deck or a collection, obviously it's a single card. But if you're representing it on a website such as Gatherer or Scryfall, it could make sense to represent it as two cards. After all, it has two names, two mana costs, two text boxes. If the rest of your program is centered around an entity called a "card", and each card has one of each of those fields, then it would be awfully tempting to model one card named Fire and another named Ice. But then what happens if you wind up with a card named "Fire" in a tournament deck list? Should that be interpreted to mean Fire/Ice, despite only half of its name being present? Do you throw an error?

These are questions that a well-factored program can answer clearly, especially under an object-oriented paradigm. So, in Lambdagoyf, I set out to take a "no compromises" approach to modeling every card with the best possible level of specificity.

### Solution

Here is a brief tour of the classes that Lambdagoyf uses to represent cards:

- **Card:** A mechanically unique card with a distinct English name. If two cards behave the same under the rules of the game, they're the same `Card`.
- **CardFace:** A mechanically unique entity with the familiar card fields: name, text, and so on. Fire/Ice is a `Card`, but Fire is one `CardFace` and Ice is another. Every `Card` is composed of one or two `CardFace`s.
- **CardEdition:** A published version of a card from a particular set, with a distinct illustration, collector number, and language. If two cards are visually identical, they're the same `CardEdition`.
- **CardEditionFace:** A card face from a particular edition. Similarly to `CardFace`, every `CardEdition` is composed of one or two `CardEditionFace`s that may represent their distinct artists, flavor text, etc.
- **PaperCard:** An instance of a `CardEdition` printed on paper. Unlike `CardEdition`, a `PaperCard` object captures information about the card's *finish* (that is, whether it is foil or not). A foil and nonfoil card are distinct `PaperCard` objects, but `CardEdition` makes no such distinction.
- **MtgoCard:** A card that exists on the _Magic: The Gathering Online_ (MTGO) platform. It represents whether the card is foil or not, and has a unique numeric ID used in deck files.
- **ArenaCard:** A card that exists on the Arena platform.

These classes are all related to each other by composition, not inheritance. The `Card` class is the root, which contains many `CardEdition` objects, and each `CardEdition` contains its various instantiations in paper, MTGO, and Arena, if they exist. See also the `CardIdentity` and `CardVersion` interfaces, which express "is-a" relationships among these composed types.

If you import these classes into your own project, I encourage you to use the correct level of abstraction in every context. Doing so can prevent entire categories of bug.

Deck file generation
--------------------

### Motivation

For many years, I've been an enthusiastic MTGO player. The platform has been my preferred way to keep up with the metagame of _Magic_'s various competitive formats. Like many players, I relish going over a "deck dump" from a recent tournament report or metagame article and looking for decks that I'd like to play myself.

MTGO has two ways of importing deck lists. The first is the inverse of its deck-exporting feature: if you build a deck from your collection and save it to an XML or CSV file, the file can later be re-imported to your account. This export/import process preserves the particular versions of the cards that you chose when building the deck. A snippet of one of those deck files might look like this:

    <Deck xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <Cards CatID="24053" Quantity="4" Sideboard="false" Name="Blood Crypt" />
      <Cards CatID="28269" Quantity="4" Sideboard="false" Name="Thoughtseize" />
    </Deck>

The second way to import a deck list is a plain-text list of card names that you would download in a common format from the official _Magic_ website or any number of third-party sites. A snippet of one of these deck lists would look like this:

    4 Blood Crypt
    4 Thoughtseize

The distinction, of course, is that this deck list format does not contain any information about the card versions. Indeed, it would be irrelevant if it did. Such lists are often abstract deck designs offered by the author of an article, and even when they are a record of a particular deck used in a tournament, data about the card versions the player used is not routinely captured (nor is it required to be the same from round to round).

Any MTGO player who uses this importing feature will learn that, when you import a plain-text deck list with no version information, the program will choose card versions from among your collection rather haphazardly. It can have a habit of picking your least favorite card art. It can carelessly mix versions among copies of the same card, even if you own enough copies of a single version to have a matching set. New cards entering your collection can be a nuisance, because they are suddenly used in preference to another version you like better. Some players make a point of trading away all copies of a card *except* their favorite version. Players have long requested a "favorite version" feature, but none seems to be forthcoming.

The deck file generation feature of Lambdagoyf started as, essentially, my own offline implementation of the "favorite version" feature, and evolved into something far more powerful.

### Solution

The core of the feature reads an unversioned deck list and produces a versioned one. The "user interface", such as it is, interacts directly with files stored on a local disk. It scans for unversioned files and writes versioned ones at predetermined paths.

What makes this really cool is the flexible customizability. As I was growing my list of favorite versions, I noticed some patterns and realized that I could take an algorithmic approach. Patterns could be as simple as "Prefer cards with watermarks," but got as complex as "Older art is generally cooler than newer art, but I really like the new art that appears for the first time in a _Modern Masters_ set." Because I could write Java code to express these patterns in as much detail as I wanted, I eventually created what you could call a crude rules engine with a domain-specific language.

To see what I mean, have a look at the `demo/RyansMtgoDecks.java` file. It isn't a contrived example; it's the real, battle-tested code that I use in my week-to-week collection management. It's not of much use to any MTGO fan who isn't also a Java coder, but I hope it can be the foundation for more broadly useful tools one day.

An important component of the deck-generation feature is knowing how many cards are in your collection. This lets the program prioritize versions you own over ones you don't, as well as using less preferred versions if it means having a matching set in the deck. (That is: if you own two copies of your favorite version and four copies of your second-favorite version, it will use the first version if the deck runs one or two copies of the card, and the second version if the deck runs three or four.) MTGO has a feature for exporting an inventory of your entire collection as a CSV file, which Lambdagoyf can parse.

Most everything that I've said of MTGO also applies to Arena, with two major exceptions. The first is that Arena doesn't have a native feature to export your collection. The second is that Arena treats alternate art and frame styles within the same set as the same card object for purposes of exporting and importing, which means we can't distinguish among them.

Project status
==============

If you're interested in using this software
-------------------------------------------

Although Lambdagoyf is set up as a library for use in your own Java code, it is very much in a beta state. If you wish to introduce a dependency on it in your own project, please:

- Consider contacting me through GitHub to let me know you are using it! If increased stability would help your work, I would like to hear about it and will do what I can to help.
- Be aware that all class and package names may be unstable between versions. In particular, I would like to get it set up under a catchy project name, and possibly change the Java package name to match the domain of a project website.
- Expect that updates to Scryfall data may cause instability in the ingestion behavior. The code is *generally* written to be tolerant of unexpected inputs (see the `Word` class for a way to match novel values to Java `enum` types) but is strict about certain things such as card types (the introduction of the "battle" type required a hard code change, as likely will the upcoming name change from "tribal" to "kindred").

Future goals
------------

- **Branding and presentation:** I'm undecided on whether "Lambdagoyf" should be a cute development codename or has legs as a long-term title. Either way, if this project garners any kind of public interest, I'd like to register a domain for it and change the Java package name accordingly. (I'm aware that cosmetic changes to package names aren't a luxury that most libraries get to enjoy. But it's a hobby project and I can do what I want.)
- **Organization:** The card-modeling and deck-generation features could, and probably should, be split into two separate source repositories and two separate Maven packages.
- **Publishing on a Maven repository:** Possibly blocked by package naming.
- **Unit tests:** I'm better about them at work, I swear.
- **A demo video:** I hope the explanation that I gave above, of my personal flow with the deck-generation tool, was clear. But I think it could be much better expressed through a visual demo.
- **JavaDoc pages:** Write JavaDoc comments where they are missing; export JavaDocs as web pages.
- **Offering version fixes upstream:** As you can see in the `MtgoIdFix` class and `resources/fix/mtgo` directory, I've found a number of Scryfall entries that are missing MTGO ID numbers. I'd like to submit those as possible updates to Scryfall's database, and maybe even help out with some systematic tools for pulling data down from the MTGO client. (Now that this page is public, perhaps some folks from Scryfall are reading right now. If so, hi! Drop me a line.)
- **A web-based and/or graphical interface for the deck customizer:** It was never my intent for the deck customization feature to be accessible only to Java programmers, but that's the state of it. The bulk reading and writing of local deck files is a good flow, and could be accomplished by a desktop application. A webapp would have its own uses. Either way, a reasonably friendly UI for users to define their own deck-generation rules (which I've so far been able to do only by writing Java code) would be immensely valuable.

License
=======

This project and its documentation are open source and licensed under the [Apache License, Version 2.0][license].

  [license]: ./LICENSE.txt

**Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.**

© 2024 Ryan Skonnord
