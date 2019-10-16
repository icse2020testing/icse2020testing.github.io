adb_path = "/Users/felicitia/Documents/Develop_Tools/android-sdk-macosx/platform-tools/adb"
aapt_path = "/Users/felicitia/Documents/Develop_Tools/android-sdk-macosx/build-tools/23.0.0/aapt"
picviewer_path = "<pic viewer>"
grepper = "grep"
apk_path = "../apks/"
tempdir = "../tmp/"

# operational params
use_my_dump = False
use_uimon = True
use_wait_idle_cmd = False
use_idle_script = False
show_guess_tags = True
threads = 8
parallel = True
dump_err_page = False
show_ocr_result = False
recall_fails = True
observe_remove_hidden_ocr = True
use_old_capture_method = True
use_fast_grab = False
WATCHDOG_TIMEOUT = 600

# optimizations
continue_testing = True
read_only_continue = True
do_exploration = False
do_init_clean = False
allow_restart = False
use_postprocess = True
use_firststep_filter = False

# ML params
classify_use_bound = False
SCREEN_SCORE_BOUND = -0.5
elements_use_bound = True
ELEMENTS_SCORE_BOUND = -0.5
elements_use_ocr = True
REGION_SCORE_BOUND = -0
region_use_ocr = True

# device props
width = 1080
height = 1920
real_height = 1794
real_height_nostatus = 1731
dialog_min_height = 200
ocr_resolution = 140

# test limits
ERROR_LIMIT = 1
TOTAL_FAIL_LIMIT = 1
SCREEN_FAIL_LIMIT = 1
TRUST_SUCC_TIMES = 1
ERR_CAP_LIMIT = 20
ROUTE_FAIL_LIMIT = 3
HANDLE_SYS_LIMIT = 5

# observe limits
GRAB_WEBVIEW = True
GRAB_RETRYCOUNT = 25
GRAB_ACT_RETRY_LIMIT = 10
GRAB_XML_RETRY_LIMIT = 3
GRAB_SCREEN_RETRY_LIMIT = 3
GRAB_SCREEN_SNAPSHOT_RETRY = 2
PER_PAGE_CAPTURE_LIMIT = 20
WEBVIEW_GRAB_LIMIT = 50

# action limits
WAITFOR_RETRY_LIMIT = 10
WAITREADY_RETRY_LIMIT = 25
CHECK_RETRY_LIMIT = 3
SCROLL_RETRY_LIMIT = 30
MAX_CLEAR_CHARS = 3
CLEAR_ONCE_CHARS = 30
NOTFOUND_SCROLLDOWN_LIMIT = 2
NOTFOUND_SCROLLUP_LIMIT = 3
WAITIDLE_MAXTRY = 25
WAITIDLE_PIXDIFF = 400
OB_EXTRA_WAIT_IDLE_LIMIT = 25
KBDACTION_KEYBOARD_LIMIT = 5
KBDSWITCH_LIMIT = 10
CHECK_OTHER_WINDOWS = True
EXPECT_LIMIT = 2

# observe & clean
CLEANUP_TRY_LIMIT = 5
CLEANUP_TRY_LIMIT_CURR = 1
CLEANUP_SYNTH_TRY_LIMIT = 3
CLEANUP_ALLINONE_BACK_LIMIT = 5
OBSERVE_TRY_LIMIT = 5
SYNTHESIS_STATE_REP = 3

# placeholder
must_cleanup_keys = []
cleanup_dep_keys = []
no_scroll_tags = []
init_state = {}
restart_state_change = {}

# analyze post-processing rules
only_double_containers = False
MERGE_NEIGHBOUR = False
REMOVE_SINGLE_CHILD_CONTAINER = True
MERGE_IMAGE_AND_TEXT_LEAF = False
REMOVE_EMPTY_CONTAINER = True
REMOVE_NEST_CLICK = False
KEEP_ONLY_FOREGROUND = True
KEEP_ONLY_FOREGROUND_H = False
REMOVE_ALPHA_OVERLAY = True
MERGE_WEBVIEW_LABEL = True
CONVERT_WEBVIEW_CLASS = True
REMOVE_BOTSLIDE_BACKGROUND = False
REMOVE_OVERLAPPING = False
FRAME_LAYOUT_CHILD = False
REMOVE_SMALL_LEAF = True
REMOVE_OVERLAP_OLD = True
REMOVE_OUT_OF_SCREEN = True

extra_screens = "signin,register,welcome,search,menu"
extra_element_scrs = "signin,register,welcome,search,menu"
