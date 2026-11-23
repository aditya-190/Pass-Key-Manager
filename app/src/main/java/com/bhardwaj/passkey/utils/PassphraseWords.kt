package com.bhardwaj.passkey.utils

import com.bhardwaj.passkey.domain.model.PasswordPolicy
import java.security.SecureRandom
import java.util.Random

/**
 * Passphrases (6.0.0): words a person can read and type, joined by hyphens. The list is PassKey's own
 * (1758 common English words, 3–9 letters, ≈10.8 bits each), so it carries no third-party licence.
 * Five words give about 54 bits.
 */
object PassphraseGenerator {
    /** The same unseeded [SecureRandom] reasoning as [PasswordGenerator]. */
    private val secureRandom: Random = SecureRandom()

    private val wordList: List<String> by lazy { WORDS.split(' ').filter { it.isNotEmpty() } }

    val words: List<String> get() = wordList

    /**
     * [capitalize] capitalises every word; [withDigit] appends one digit to one word, for sites that demand a
     * number. [random] is injectable for tests only.
     */
    fun generate(words: Int, capitalize: Boolean, withDigit: Boolean, random: Random = secureRandom): String {
        val count = words.coerceIn(PasswordPolicy.MIN_WORDS, PasswordPolicy.MAX_WORDS)
        val picked = MutableList(count) {
            val word = wordList[random.nextInt(wordList.size)]
            if (capitalize) word.replaceFirstChar(Char::uppercaseChar) else word
        }
        if (withDigit) {
            val index = random.nextInt(count)
            picked[index] = picked[index] + random.nextInt(DIGITS)
        }
        return picked.joinToString(separator = "-")
    }

    private const val DIGITS = 10
}

private const val WORDS =
    "able about above acid acorn acre actor adapt adore adult advice afford after again agenda agent " +
        "agree ahead aim air airport alarm album alert alien alive alley allow almanac almond aloe alone " +
        "along alpha amber amount ample amuse anchor angel angle animal ankle answer anthem apex apple apron " +
        "arch arena argue arm armor army aroma arrow art artist ash aside ask atlas atom atrium attic audio " +
        "aunt aura autumn avenue avid avocado awake award axis baby back bacon badge bag bagel bake balance " +
        "balcony ball ballad bamboo banana band banjo bank barley barn barrel base basket bath baton battery " +
        "bazaar beach beacon beam bean bear beard beast beauty become bed bee beef beetle begin behave bell " +
        "belt bench berry best better bicycle bingo bird birth biscuit bistro bit black blade blanket blaze " +
        "blend blink block bloom blossom blue board boat bobcat body boil bolt bone bongo bonsai bonus book " +
        "boost boot border borrow boss bottle bottom boulder bounce bowl box brain bramble branch brave " +
        "bread break breeze brick bridge brief bright bring brook broom brother brown brunch brush bubble " +
        "bucket buckle budget buffalo bugle build bulb bundle bunny burger burrow bus bush butler butter " +
        "button buyer buzz cabin cable cactus cadet cage cake calico calm camel camera camp camper canal " +
        "canary candle candor candy canoe canvas canyon cap captain car caramel caravan carbon card cardinal " +
        "cargo carnival carpet carrot cart case cash cashew castle cat catch cattle cause cave cedar ceiling " +
        "celery cell cello cement cereal chain chair chalk champion change chapel chapter charge charm chart " +
        "chase cheap check cheese chef cherry cherub chess chest chicken chief child chili chimney choice " +
        "chord chorus cider cinder cinema circle citizen citrus city civil claim clap class clay clean clerk " +
        "click cliff climb clock close cloth cloud clover clown club coach coast coat cobalt cobra cocoa " +
        "coconut code coffee coin cold collar color column comet comfort comic comma common condor cookie " +
        "copper coral corn corner cosmos cotton couch country couple course cousin cove cover cow coyote " +
        "crab craft crane crater crayon cream credit creek crest crew cricket crisp crop cross crowd crown " +
        "crumb crystal cube cuckoo cumin cup cupboard curry curve cushion custom cycle dahlia daily dairy " +
        "daisy dance dancer danger dark dash data date dawn day deal debate decade deck decor decoy deer " +
        "degree delay deliver delta demand denim depth deputy desert design desk detail device dewdrop dial " +
        "diamond diary diet digit dimple dingo dinner diploma dish distant diver dock doctor dog doll " +
        "dolphin domain donkey doodle door dorm dot double dough dove dozen draft dragon drama drawer dream " +
        "dress drift drill drink drive drop drum duck dune dust duty dynamo eager eagle early earn earth " +
        "easel east easy echo eclipse edge editor effort egg eight elbow elder element elephant elevator " +
        "eleven elixir elk elm email ember emblem emerald empty emu enable enamel end energy engine enjoy " +
        "enough enter entry envelope epic equal equator era ermine errand escape essay estate estuary " +
        "evening event exact exam example exit expert extra eye fable fabric face fact factor fair fairy " +
        "faith falafel falcon fame family fancy farm fashion fast father fault feast feather fee fence fern " +
        "ferry festival fever fiber fiddle field fig figure film filter final finch finger finish fire firm " +
        "fish fitness five fjord flag flame flamingo flannel flash flat flavor fleet flight flint float " +
        "flock floor flour flower fluid flurry flute foam focus fog fold folk fondue food foot forest fork " +
        "form fort forward fossil fox foyer frame fresh friend frog front frost fruit fudge fuel fun funny " +
        "fur fusion future gadget gala galaxy galleon gallery game garage garden garlic garnet gas gate " +
        "gather gauge gazebo gear gecko gem general genius gentle geyser ghost giant gift ginger gingham " +
        "giraffe give glacier glad glass glider globe glove glow glue gnome goal goat goblet gold golf " +
        "gondola good goose gorilla gospel gourd grace grade grain grand granite grape graph grass gravel " +
        "gravity gravy great green grid griffin grill grin grocery ground group grove grow guard guess guest " +
        "guide guitar gulf gumbo gusto gym habit hair half hall halo hammer hammock hamster hand handle " +
        "happy harbor hard harp harvest hat haven hawk hazard hazel head health heart heat heather hedge " +
        "height hello helmet help hemlock hermit hero heron hidden high hike hill hint hip hippo history " +
        "hobby hockey hold holiday hollow home honey hood hook hope horizon horn hornet horse hostel hotel " +
        "hound hour house hover huddle human humble humor hundred hunt hurry husband husky hut hymn ice " +
        "iceberg icon idea igloo iguana image impact inch index indoor infant ink inkwell inlet input insect " +
        "inside invite iris iron island issue itch item ivory ivy jackal jacket jaguar jam jar jasmine " +
        "javelin jazz jeans jelly jester jewel jigsaw job jockey join joke jolly journal joy jubilee judge " +
        "juice jump jungle junior juniper jury just kangaroo kayak keen keep kelp kennel kernel ketchup " +
        "kettle key keyboard kick kid kidney kind king kingdom kiosk kit kitchen kite kitten kiwi knee knife " +
        "knight knock knot know koala label labor lace ladder lady lagoon lake lamb lamp land lane language " +
        "lantern laptop large laser lasso latch later lattice laugh laurel lava lawn layer leader leaf learn " +
        "leather lecture left legend lemon lemur length lens leopard lesson letter level lever liberty " +
        "library license life lift light lilac lily limb lime limit linden linen lion liquid list little " +
        "live lizard load loaf lobby lobe lobster local lock locket locust lodge logic long loop lotus loud " +
        "lounge love loyal lucky lullaby lumber lunar lunch lynx macaw machine magic magnet magpie maid mail " +
        "major mallard mammal mammoth mango manor mantis maple marble margin marina marine market marlin " +
        "marsh marshal mascot mask mason master match matter maze meadow meal measure medal media melody " +
        "melon member memory mental menu merit mesh metal meteor method middle midnight milk mill mimic mind " +
        "mineral minnow minute mirror mist mitten mixer mocha model modern mohair molar moment mongoose " +
        "monkey month moon moose moral morning morsel mosaic moss motion motor mountain mouse mouth movie " +
        "muesli muffin mule mural museum music mussel mustard mutual myth nachos nail name napkin narrow " +
        "nation native nature navy near neat neck nectar needle neighbor nephew nest net network never newt " +
        "next nice nickel niece night nimble nine noble noise nomad noodle normal north nose note notice " +
        "nougat novel number nurse nut nutmeg nylon oak oasis oatmeal object oboe ocean ocelot octave odor " +
        "offer office often olive omega omelet onion online onyx open opera option orange orbit orchard " +
        "orchid order organ origin oriole osprey otter ounce outdoor outer outpost oval oven owl owner " +
        "oxygen oyster ozone pace pack paddle paddock page pagoda paint pair palace palm panda panel panic " +
        "paper paprika parade parcel parent park parrot parsley party pass pasta paste pastry patch path " +
        "patio patrol pattern pause peace peach peacock peak peanut pear pearl pebble pecan pedal pelican " +
        "pelt pen pencil penguin people pepper perfect period person pet petal pewter phone photo piano " +
        "pickle picnic picture piece pig pigeon pillow pilot pine pink pinwheel pioneer pipe piper pistachio " +
        "pitch pizza place plaid plain planet plant plate platypus play plaza plenty pliers plot plum plume " +
        "plus pocket poem poet point polar pole policy polish polka pompom poncho pond pony pool popcorn " +
        "poppy porch port possum post potato pottery pound powder power prairie praise prawn prefer present " +
        "press pretzel price pride prime prince print prism prize profit program project proof proud public " +
        "puffin pug pulse pump pumpkin pupil puppy purple puzzle pyramid quail quality quarter quartz queen " +
        "query quest quiche quick quiet quilt quokka quote rabbit raccoon race radar radiant radio radish " +
        "raft ragtime rail rain rainbow raise raisin ranch random range rapid rattle raven razor reach ready " +
        "real reason rebel recipe record red redwood reef region relax relic remote rent reply report rescue " +
        "result retire return reward rhino rhythm ribbon rice rich riddle ride ridge right ring ripple river " +
        "riverbed road robin robot rock rocket roof room rooster root rope rose rosemary rotate round route " +
        "rowboat royal rubber ruby rug ruler rumor runway rural rustic saddle safe safety saffron sail salad " +
        "salmon salsa salt sample sand sandal sapphire sardine satin sauce sausage savanna saver scale " +
        "scallop scarf scene school science scooter score scout screen script sea seal season seat second " +
        "secret section seed segment senior sense sequin series sesame seven shadow shape share shark sheep " +
        "shelf shell shelter sherbet shield shift shine ship shirt shoe shore short shoulder shovel shower " +
        "shrimp sierra sign signal silk silver simple siren sister sixty skate sketch ski skill skirt sky " +
        "skylark sled sleep sleeve slice slide slope sloth smart smile smoke snack snail snake snow soap " +
        "soccer sock sofa soft solar soldier solid song sorbet sound soup south space spare spark speak " +
        "speed spell spice spider spike spinach spirit split sponge spoon sport spot spray spring sprout " +
        "spruce square squash squid stable stadium staff stage stair stamp star starling state station " +
        "statue steam steel stem step stereo stick stock stone stool storm story stove straw stream street " +
        "strong student studio style sugar suit summer summit sun sundial sunset super supply surf surface " +
        "sushi swamp swan sweater sweet swift swim swing switch sycamore symbol syrup system table tablet " +
        "tackle taffy tail talent tango tank tape tapir target taste tavern taxi tea teach teal team teapot " +
        "teddy teeth tennis tent term termite test text thank theme theory thick thing thirty thistle thread " +
        "three thumb thunder thyme ticket tidal tide tiger timber time tinsel tiny tissue title toast today " +
        "toe toffee tomato tomorrow tone tongue tool tooth topaz topic torch tortoise total toucan tourist " +
        "towel tower town toy track tractor trade traffic trail train travel tray treat tree trellis trend " +
        "trial tribe trick trip trophy trout truck true trumpet trunk trust truth tulip tuna tundra tunnel " +
        "turkey turn turnip turtle tuxedo twelve twenty twin type ukulele umbrella umpire uncle under " +
        "unicorn uniform union unique unison unit universe update upper urban usage useful usual vacuum " +
        "valley value valve van vanilla vapor vase vault vector velvet vendor venue verb verse vessel vest " +
        "video view village vine vinyl violet violin virtue visa visit vista visual vital vivid vocal voice " +
        "volcano volume voter voyage vulture wafer waffle wagon waist walk wall walnut walrus wander warbler " +
        "warm wash wasp water wave wax way wealth weather web wedding week weight welcome west wet whale " +
        "wheat wheel whisper whistle white wide width wife wild willow wind window wing winner winter wire " +
        "wisdom wish witness wizard wolf wombat wonder wood wool word work world worth wreath wren wrist " +
        "writer yacht yak yard year yellow yodel yogurt young youth yoyo zebra zeppelin zero zest zigzag " +
        "zinc zipper zodiac zone zoo zucchini "
