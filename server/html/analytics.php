<head>
<script type="text/javascript" src="https://cdn.jsdelivr.net/jquery/latest/jquery.min.js"></script>
<script type="text/javascript" src="https://cdn.jsdelivr.net/momentjs/latest/moment.min.js"></script>
<script type="text/javascript" src="https://cdn.jsdelivr.net/npm/daterangepicker/daterangepicker.min.js"></script>
<script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
<link rel="stylesheet" type="text/css" href="https://cdn.jsdelivr.net/npm/daterangepicker/daterangepicker.css" />
</head>
<body>
<?php

ini_set('display_errors', 1);

require_once 'game_db_include.php';

$PLAY_TIME_0_TO_1_MIN    = 0;
$PLAY_TIME_1_TO_5_MIN    = 1;
$PLAY_TIME_5_TO_15_MIN   = 2;
$PLAY_TIME_15_TO_30_MIN  = 3;
$PLAY_TIME_30_TO_60_MIN  = 4;
$PLAY_TIME_1_TO_2_HOUR   = 5;
$PLAY_TIME_2_TO_6_HOUR   = 6;
$PLAY_TIME_6_TO_20_HOUR  = 7;
$PLAY_TIME_20_PLUS_HOUR  = 8;
$PLAY_TIME_NUM_BUCKETS   = 9;

$LOGIN_1_TIMES        = 0;
$LOGIN_2_TIMES        = 1;
$LOGIN_3_TIMES        = 2;
$LOGIN_4_TIMES        = 3;
$LOGIN_5_TO_9_TIMES   = 4;
$LOGIN_10_PLUS_TIMES  = 5;
$LOGIN_NUM_BUCKETS    = 6;

$CHATTED_YES          = 0;
$CHATTED_NO           = 1;
$CHATTED_NUM_BUCKETS  = 2;

$VIEWED_MAINLAND_ONLY          = 0;
$VIEWED_MAINLAND_HOMELAND      = 1;
$VIEWED_MAINLAND_HOMELAND_RAID = 2;
$VIEWED_NUM_BUCKETS            = 3;

$XP_0              = 0;
$XP_1_TO_20        = 1;
$XP_20_to_100      = 2;
$XP_100_TO_500     = 3;
$XP_500_TO_1000    = 4;
$XP_1000_TO_5000   = 5;
$XP_5000_TO_50000  = 6;
$XP_50000_PLUS     = 7;
$XP_NUM_BUCKETS    = 8;

$PLATFORM_STEAM        = 0;
$PLATFORM_WINDOWS      = 1;
$PLATFORM_ANDROID      = 2;
$PLATFORM_UNKNOWN      = 3;
$PLATFORM_NUM_BUCKETS  = 4;

$TOD_0_TO_3      = 0;
$TOD_3_TO_6      = 1;
$TOD_6_TO_9      = 2;
$TOD_9_TO_12     = 3;
$TOD_12_TO_15    = 4;
$TOD_15_TO_18    = 5;
$TOD_18_TO_21    = 6;
$TOD_21_TO_24    = 7;
$TOD_NUM_BUCKETS = 8;

$TUTORAL_STATES_COUNT = 99;

$TUTORIAL_STATE_ID[1] = "nation_on_map";
$TUTORIAL_STATE_ID[2] = "moving_map";
$TUTORIAL_STATE_ID[3] = "past_boundary";
$TUTORIAL_STATE_ID[4] = "goal_orbs";
$TUTORIAL_STATE_ID[5] = "goal_go_east";
$TUTORIAL_STATE_ID[6] = "importance_of_attacking";
$TUTORIAL_STATE_ID[7] = "occupy_empty_1";
$TUTORIAL_STATE_ID[8] = "occupy_empty_2";
$TUTORIAL_STATE_ID[9] = "attack_1";
$TUTORIAL_STATE_ID[10] = "attack_fails";
$TUTORIAL_STATE_ID[11] = "attack_succeeds";
$TUTORIAL_STATE_ID[12] = "attack_2";
$TUTORIAL_STATE_ID[13] = "attack_3";
$TUTORIAL_STATE_ID[14] = "attack_4";
$TUTORIAL_STATE_ID[15] = "attack_5";
$TUTORIAL_STATE_ID[16] = "attack_6";
$TUTORIAL_STATE_ID[17] = "attack_7";
$TUTORIAL_STATE_ID[18] = "attack_8";
$TUTORIAL_STATE_ID[19] = "attack_9";
$TUTORIAL_STATE_ID[20] = "defense_1";
$TUTORIAL_STATE_ID[21] = "defense_2";
$TUTORIAL_STATE_ID[22] = "defense_3";
$TUTORIAL_STATE_ID[23] = "defense_4";
$TUTORIAL_STATE_ID[24] = "defense_5";
$TUTORIAL_STATE_ID[25] = "defense_6";
$TUTORIAL_STATE_ID[26] = "defense_7";
$TUTORIAL_STATE_ID[27] = "defense_8";
$TUTORIAL_STATE_ID[28] = "defense_9";
$TUTORIAL_STATE_ID[29] = "defense_10";
$TUTORIAL_STATE_ID[30] = "defense_11";
$TUTORIAL_STATE_ID[31] = "defense_12";
$TUTORIAL_STATE_ID[32] = "defense_13";
$TUTORIAL_STATE_ID[33] = "defense_14";
$TUTORIAL_STATE_ID[34] = "defense_15";
$TUTORIAL_STATE_ID[35] = "defense_16";
$TUTORIAL_STATE_ID[36] = "defense_17";
$TUTORIAL_STATE_ID[37] = "defense_18";
$TUTORIAL_STATE_ID[38] = "quests_and_credits";
$TUTORIAL_STATE_ID[39] = "summary_1";
$TUTORIAL_STATE_ID[40] = "summary_2";
$TUTORIAL_STATE_ID[41] = "geo_eff_1_1";
$TUTORIAL_STATE_ID[42] = "geo_eff_1_2";
$TUTORIAL_STATE_ID[43] = "geo_eff_2_1";
$TUTORIAL_STATE_ID[44] = "geo_eff_2_2";
$TUTORIAL_STATE_ID[45] = "geo_eff_2_3";
$TUTORIAL_STATE_ID[46] = "geo_eff_3";
$TUTORIAL_STATE_ID[47] = "geo_eff_4";
$TUTORIAL_STATE_ID[48] = "occupy_area";
$TUTORIAL_STATE_ID[49] = "evacuate_area";
$TUTORIAL_STATE_ID[50] = "occupy_empty";
$TUTORIAL_STATE_ID[51] = "mountain_paths";
$TUTORIAL_STATE_ID[52] = "resources";
$TUTORIAL_STATE_ID[53] = "orbs";
$TUTORIAL_STATE_ID[54] = "next_level_boundary";
$TUTORIAL_STATE_ID[55] = "upgrade_defense";
$TUTORIAL_STATE_ID[56] = "level_up_1";
$TUTORIAL_STATE_ID[57] = "level_up_2";
$TUTORIAL_STATE_ID[58] = "level_up_3";
$TUTORIAL_STATE_ID[59] = "level_up_4";
$TUTORIAL_STATE_ID[60] = "level_up_5";
$TUTORIAL_STATE_ID[61] = "map_panel_1";
$TUTORIAL_STATE_ID[62] = "map_panel_2";
$TUTORIAL_STATE_ID[63] = "create_password";
$TUTORIAL_STATE_ID[64] = "recruit";
$TUTORIAL_STATE_ID[65] = "surround_count";
$TUTORIAL_STATE_ID[66] = "energy_reserves_decreasing";
$TUTORIAL_STATE_ID[67] = "energy_reserves_empty";
$TUTORIAL_STATE_ID[68] = "excess_resources";
$TUTORIAL_STATE_ID[69] = "home_island";
$TUTORIAL_STATE_ID[70] = "homeland_1";
$TUTORIAL_STATE_ID[71] = "homeland_2";
$TUTORIAL_STATE_ID[72] = "homeland_3";
$TUTORIAL_STATE_ID[73] = "homeland_4";
$TUTORIAL_STATE_ID[74] = "homeland_5";
$TUTORIAL_STATE_ID[75] = "homeland_6";
$TUTORIAL_STATE_ID[76] = "homeland_7";
$TUTORIAL_STATE_ID[77] = "homeland_8";
$TUTORIAL_STATE_ID[78] = "homeland_9";
$TUTORIAL_STATE_ID[79] = "homeland_10";
$TUTORIAL_STATE_ID[80] = "homeland_11";
$TUTORIAL_STATE_ID[81] = "raid_1";
$TUTORIAL_STATE_ID[82] = "raid_2";
$TUTORIAL_STATE_ID[83] = "raid_3";
$TUTORIAL_STATE_ID[84] = "raid_4";
$TUTORIAL_STATE_ID[85] = "raid_5";
$TUTORIAL_STATE_ID[86] = "raid_6";
$TUTORIAL_STATE_ID[87] = "raid_7";
$TUTORIAL_STATE_ID[88] = "raid_8";
$TUTORIAL_STATE_ID[89] = "raid_9";
$TUTORIAL_STATE_ID[90] = "raid_10";
$TUTORIAL_STATE_ID[91] = "raid_11";
$TUTORIAL_STATE_ID[92] = "raid_12";
$TUTORIAL_STATE_ID[93] = "raid_13";
$TUTORIAL_STATE_ID[94] = "raid_14";
$TUTORIAL_STATE_ID[95] = "raid_log_1";
$TUTORIAL_STATE_ID[96] = "raid_log_2";
$TUTORIAL_STATE_ID[97] = "raid_log_3";
$TUTORIAL_STATE_ID[98] = "raid_log_4";

$yesterday_date_string = date("n/j/y", time() - (24 * 3600));

$play_time_filter = -1;
if (isset($_GET['play_time_filter'])) {
	$play_time_filter = $_GET['play_time_filter'];
}

echo "<form action=\"/analytics.php\" method=\"get\">";

if (isset($_GET['daterange'])) {
	echo "<input type=\"text\" name=\"daterange\" value=\"".$_GET['daterange']."\" />";
} else {
	echo "<input type=\"text\" name=\"daterange\" value=\"$yesterday_date_string - $yesterday_date_string\" />";
}

echo "
<select name=\"play_time_filter\">
  <option value=\"-1\"".(($play_time_filter == -1) ? " selected" : "").">All play times</option>
	<option value=\"".$PLAY_TIME_0_TO_1_MIN."\"".(($play_time_filter == $PLAY_TIME_0_TO_1_MIN) ? " selected" : "").">Played 0-1 mins</option>
	<option value=\"".$PLAY_TIME_1_TO_5_MIN."\"".(($play_time_filter == $PLAY_TIME_1_TO_5_MIN) ? " selected" : "").">Played 1-5 mins</option>
	<option value=\"".$PLAY_TIME_5_TO_15_MIN."\"".(($play_time_filter == $PLAY_TIME_5_TO_15_MIN) ? " selected" : "").">Played 5-15 mins</option>
	<option value=\"".$PLAY_TIME_15_TO_30_MIN."\"".(($play_time_filter == $PLAY_TIME_15_TO_30_MIN) ? " selected" : "").">Played 15-30 mins</option>
	<option value=\"".$PLAY_TIME_30_TO_60_MIN."\"".(($play_time_filter == $PLAY_TIME_30_TO_60_MIN) ? " selected" : "").">Played 30-60 mins</option>
	<option value=\"".$PLAY_TIME_1_TO_2_HOUR."\"".(($play_time_filter == $PLAY_TIME_1_TO_2_HOUR) ? " selected" : "").">Played 1-2 hrs</option>
	<option value=\"".$PLAY_TIME_2_TO_6_HOUR."\"".(($play_time_filter == $PLAY_TIME_2_TO_6_HOUR) ? " selected" : "").">Played 2-6 hrs</option>
	<option value=\"".$PLAY_TIME_6_TO_20_HOUR."\"".(($play_time_filter == $PLAY_TIME_6_TO_20_HOUR) ? " selected" : "").">Played 6-20 hrs</option>
	<option value=\"".$PLAY_TIME_20_PLUS_HOUR."\"".(($play_time_filter == $PLAY_TIME_20_PLUS_HOUR) ? " selected" : "").">Played 20+ hrs</option>
</select>
";

echo "<input type=\"submit\" value=\"Apply\">";
echo "</form>";

if (isset($_GET['daterange']))
{
	$first_date = substr($_GET['daterange'], 0, strpos($_GET['daterange'], " - "));
	$second_date = substr($_GET['daterange'], strpos($_GET['daterange'], " - ") + 3);
	$first_date_time = strtotime($first_date);
	$second_date_time = strtotime($second_date) + (24 * 3600); // Go to end of day second date.
	$num_days = round(($second_date_time - $first_date_time) / (24 * 3600));

	$min_play_time = 0;
	$max_play_time = 4000000000;
	switch ($play_time_filter)
	{
		case $PLAY_TIME_0_TO_1_MIN: $min_play_time = 0; $max_play_time = 60; break;
		case $PLAY_TIME_1_TO_5_MIN: $min_play_time = 60; $max_play_time = 5 * 60; break;
		case $PLAY_TIME_5_TO_15_MIN: $min_play_time = 5 * 60; $max_play_time = 15 * 60; break;
		case $PLAY_TIME_15_TO_30_MIN: $min_play_time = 15 * 60; $max_play_time = 30 * 60; break;
		case $PLAY_TIME_30_TO_60_MIN: $min_play_time = 30 * 60; $max_play_time = 60 * 60; break;
		case $PLAY_TIME_1_TO_2_HOUR: $min_play_time = 3600; $max_play_time = 2 * 3600; break;
		case $PLAY_TIME_2_TO_6_HOUR: $min_play_time = 2 * 3600; $max_play_time = 6 * 3600; break;
		case $PLAY_TIME_6_TO_20_HOUR: $min_play_time = 6 * 3600; $max_play_time = 20 * 3600; break;
		case $PLAY_TIME_20_PLUS_HOUR: $min_play_time = 20 * 3600; $max_play_time = 4000000000; break;
	}

	//echo "Daterange: ".$_GET['daterange'];
	//echo "first date: ".$first_date.", second date: ".$second_date." first time: ".$first_date_time.", second time: ".$second_date_time." cur date 10/15/18 cur date time: ".strtotime("10/15/18")." cur time:".time();

	// Connect to MySQL and select the database
	$db = mysql_connect($game_db_host, $game_db_username, $game_db_password);

	if (!$db) {
			die('Could not connect: ' . mysql_error());
	}

	$select = mysql_select_db($game_db_database, $db);

	if (!$select) {
			die('Could not select: ' . mysql_error());
	}

	// Query database for user data.
	// Skip those with login_count 0, because they are not player accounts, they're demo/feeder/nemesis account.
	$query_string = "SELECT * FROM Users WHERE login_count > 0 AND creation_time >= '".$first_date_time."' AND creation_time <= '".$second_date_time."' AND play_time >= '".$min_play_time."' AND play_time <= '".$max_play_time."'";

	$result = mysql_query($query_string);

	$num_new_users = mysql_num_rows($result);
	echo "There were ".$num_new_users." matching new users in this ".$num_days." day period.";

	$new_user_per_day = array_fill(0, $num_days, 0);
	$play_time_buckets = array_fill(0, $PLAY_TIME_NUM_BUCKETS, 0);
	$login_count_buckets = array_fill(0, $LOGIN_NUM_BUCKETS, 0);
	$tutorial_state_buckets = array_fill(0, $TUTORAL_STATES_COUNT, 0);
	$chatted_buckets = array_fill(0, $CHATTED_NUM_BUCKETS, 0);
	$viewed_buckets = array_fill(0, $VIEWED_NUM_BUCKETS, 0);
	$xp_buckets = array_fill(0, $XP_NUM_BUCKETS, 0);
	$platform_buckets = array_fill(0, $PLATFORM_NUM_BUCKETS, 0);
	$tod_buckets = array_fill(0, $TOD_NUM_BUCKETS, 0);

	// Gather data
	while ( $row = mysql_fetch_array($result) ) 
	{
    //echo $row['name']."</br>\n";
		$day_index = intval(($row['creation_time'] - $first_date_time) / (24 * 3600));

		// New users per day
		$new_user_per_day[$day_index] = $new_user_per_day[$day_index] + 1;

		// Play time
		if ($row['play_time'] <= 60) {
			$play_time_buckets[$PLAY_TIME_0_TO_1_MIN]++;
		} else if ($row['play_time'] <= (5 * 60)) {
			$play_time_buckets[$PLAY_TIME_1_TO_5_MIN]++;
		} else if ($row['play_time'] <= (15 * 60)) {
			$play_time_buckets[$PLAY_TIME_5_TO_15_MIN]++;
		} else if ($row['play_time'] <= (30 * 60)) {
			$play_time_buckets[$PLAY_TIME_15_TO_30_MIN]++;
		} else if ($row['play_time'] <= (60 * 60)) {
			$play_time_buckets[$PLAY_TIME_30_TO_60_MIN]++;
		} else if ($row['play_time'] <= (2 * 3600)) {
			$play_time_buckets[$PLAY_TIME_1_TO_2_HOUR]++;
		} else if ($row['play_time'] <= (6 * 3600)) {
			$play_time_buckets[$PLAY_TIME_2_TO_6_HOUR]++;
		} else if ($row['play_time'] <= (20 * 3600)) {
			$play_time_buckets[$PLAY_TIME_6_TO_20_HOUR]++;
		} else {
			$play_time_buckets[$PLAY_TIME_20_PLUS_HOUR]++;
		}

		// Login count
		if ($row['login_count'] == 1) {
			$login_count_buckets[$LOGIN_1_TIMES]++;
		} else if ($row['login_count'] == 2) {
			$login_count_buckets[$LOGIN_2_TIMES]++;
		} else if ($row['login_count'] == 3) {
			$login_count_buckets[$LOGIN_3_TIMES]++;
		} else if ($row['login_count'] == 4) {
			$login_count_buckets[$LOGIN_4_TIMES]++;
		} else if (($row['login_count'] >= 5) && ($row['login_count'] <= 9)) {
			$login_count_buckets[$LOGIN_5_TO_9_TIMES]++;
		} else  {
			$login_count_buckets[$LOGIN_10_PLUS_TIMES]++;
		}

		// Tutorial state
		//echo $row['tutorial_state']."</br>\n";
		$json_array = "{\"".substr(str_replace(":", "\":", str_replace(",", ",\"", $row['tutorial_state'])), 0, -2)."}";
		$decoded_json = json_decode($json_array);
		$decoded_json = (array)$decoded_json;
		$add_2_defense_lessons = ($second_date_time < strtotime("10/30/2018")); // Determine whether the two new defense lessons need to be adjusted for.
		if (is_array($decoded_json))
		{
			foreach ($decoded_json as $key => $value) 
			{
				$lesson_index = intval($key);

				if ($add_2_defense_lessons && ($lesson_index >= 24)) {
					$lesson_index += 2;
				}

				$tutorial_state_buckets[$lesson_index]++;
			}
		}

		// Chatted
		//echo "prev_chat_fine_time: ".$row['prev_chat_fine_time'].", prev_login_time: ".$row['prev_login_time'].", abs dif: ".abs(($row['prev_chat_fine_time'] / 1000) - $row['prev_login_time'])."</br>\n";
		if (abs(($row['prev_chat_fine_time'] / 1000) - $row['prev_login_time']) < 2) {
			$chatted_buckets[$CHATTED_NO]++;
		} else {
			$chatted_buckets[$CHATTED_YES]++;
		}

		// Viewed
		if ($row['raidland_viewX'] != 0) {
			$viewed_buckets[$VIEWED_MAINLAND_HOMELAND_RAID]++;
		} else if ($row['homeland_viewX'] != 0) {
			$viewed_buckets[$VIEWED_MAINLAND_HOMELAND]++;
		} else {
			$viewed_buckets[$VIEWED_MAINLAND_ONLY]++;
		}

		// XP
		if ($row['xp'] == 0) {
			$xp_buckets[$XP_0]++;
		} else if ($row['xp'] <= 20) {
			$xp_buckets[$XP_1_TO_20]++;
		} else if ($row['xp'] <= 100) {
			$xp_buckets[$XP_20_to_100]++;
		} else if ($row['xp'] <= 500) {
			$xp_buckets[$XP_100_TO_500]++;
		} else if ($row['xp'] <= 1000) {
			$xp_buckets[$XP_500_TO_1000]++;
		} else if ($row['xp'] <= 5000) {
			$xp_buckets[$XP_1000_TO_5000]++;
		} else if ($row['xp'] <= 50000) {
			$xp_buckets[$XP_5000_TO_50000]++;
		} else {
			$xp_buckets[$XP_50000_PLUS]++;
		}

		// Platform
		if (stripos($row['creation_device_type'], "steam") !== false) {
			$platform_buckets[$PLATFORM_STEAM]++;
		} else if (stripos($row['creation_device_type'], "windows") !== false) {
			$platform_buckets[$PLATFORM_WINDOWS]++;
		} else if (stripos($row['creation_device_type'], "android") !== false) {
			$platform_buckets[$PLATFORM_ANDROID]++;
		} else {
			$platform_buckets[$PLATFORM_UNKNOWN]++;
		}

		// Time of day
		$hour = intval(date("G", $row['creation_time']));
		if (($hour >= 0) && ($hour < 3)) {
			$tod_buckets[$TOD_0_TO_3]++;
		} else if (($hour >= 3) && ($hour < 6)) {
			$tod_buckets[$TOD_3_TO_6]++;
		} else if (($hour >= 6) && ($hour < 9)) {
			$tod_buckets[$TOD_6_TO_9]++;
		} else if (($hour >= 9) && ($hour < 12)) {
			$tod_buckets[$TOD_9_TO_12]++;
		} else if (($hour >= 12) && ($hour < 15)) {
			$tod_buckets[$TOD_12_TO_15]++;
		} else if (($hour >= 15) && ($hour < 18)) {
			$tod_buckets[$TOD_15_TO_18]++;
		} else if (($hour >= 18) && ($hour < 21)) {
			$tod_buckets[$TOD_18_TO_21]++;
		} else  {
			$tod_buckets[$TOD_21_TO_24]++;
		}
	}

	// Display charts //////////////////////////

	echo "
	<div id=\"num_new_users_chart_div\"></div>
	<div id=\"play_time_chart_div\" style=\"width: 900px; height: 500px;\"></div>
	<div id=\"login_count_chart_div\" style=\"width: 900px; height: 500px;\"></div>
	<div id=\"chatted_chart_div\" style=\"width: 900px; height: 500px;\"></div>
	<div id=\"viewed_chart_div\" style=\"width: 900px; height: 500px;\"></div>
	<div id=\"xp_chart_div\" style=\"width: 900px; height: 500px;\"></div>
	<div id=\"platform_chart_div\" style=\"width: 900px; height: 500px;\"></div>
	<div id=\"tod_chart_div\" style=\"width: 900px; height: 500px;\"></div>
	<div id=\"tutorial_state_chart_div\" style=\"width: 900px; height: 3000px;\"></div>

	<script type=\"text/javascript\">
	google.charts.load('current', {packages: ['corechart', 'line', 'bar']});
	google.charts.setOnLoadCallback(drawCharts);

	function drawCharts() {

			// Display chart of number of new users per day //////////////////////////

      var data = new google.visualization.DataTable();
			data.addColumn('date', 'Day');
      data.addColumn('number', 'New Users');

      data.addRows([
			";

	for ($i = 0; $i < $num_days; $i++)
	{
		$cur_date_time = $first_date_time + ($i * (24 * 3600));
		if ($i > 0) echo ", ";
		echo "[new Date(".date("Y, ", $cur_date_time).(date("n", $cur_date_time) - 1).date(", j", $cur_date_time)."),".$new_user_per_day[$i]."]"; // Date string is constructed in this way because javascript Date() constructor takes 0-based month index.
	}

	echo "
      ]);

      var options = {
        hAxis: {
          title: 'Date'
        },
        vAxis: {
          title: 'Number of New Users'
        },
        colors: ['#AB0D06', '#007329'],
      };

      var chart = new google.visualization.LineChart(document.getElementById('num_new_users_chart_div'));
      chart.draw(data, options);

			// Display chart of time played per user

			var data = google.visualization.arrayToDataTable([
          ['Total Play Time', 'New Users'],
        			";

	echo "['0 to 1 min', ".$play_time_buckets[$PLAY_TIME_0_TO_1_MIN]."], ";
	echo "['1 to 5 mins', ".$play_time_buckets[$PLAY_TIME_1_TO_5_MIN]."], ";
	echo "['5 to 15 mins', ".$play_time_buckets[$PLAY_TIME_5_TO_15_MIN]."], ";
	echo "['15 to 30 mins', ".$play_time_buckets[$PLAY_TIME_15_TO_30_MIN]."], ";
	echo "['30 to 60 mins', ".$play_time_buckets[$PLAY_TIME_30_TO_60_MIN]."], ";
	echo "['1 to 2 hours', ".$play_time_buckets[$PLAY_TIME_1_TO_2_HOUR]."], ";
	echo "['2 to 6 hours', ".$play_time_buckets[$PLAY_TIME_2_TO_6_HOUR]."], ";
	echo "['6 to 20 hours', ".$play_time_buckets[$PLAY_TIME_6_TO_20_HOUR]."], ";
	echo "['20+ hours', ".$play_time_buckets[$PLAY_TIME_20_PLUS_HOUR]."]";

	echo "
        ]);

      var options = {
        title: 'Total Play Time of New Users'
      };

      var chart = new google.visualization.PieChart(document.getElementById('play_time_chart_div'));

      chart.draw(data, options);

			// Display chart of login counts

			var data = google.visualization.arrayToDataTable([
          ['Logins', 'New Users'],
        			";

	echo "['1', ".$login_count_buckets[$LOGIN_1_TIMES]."], ";
	echo "['2', ".$login_count_buckets[$LOGIN_2_TIMES]."], ";
	echo "['3', ".$login_count_buckets[$LOGIN_3_TIMES]."], ";
	echo "['4', ".$login_count_buckets[$LOGIN_4_TIMES]."], ";
	echo "['5 to 9', ".$login_count_buckets[$LOGIN_5_TO_9_TIMES]."], ";
	echo "['10+', ".$login_count_buckets[$LOGIN_10_PLUS_TIMES]."]";

	echo "
			]);

			var options = {
				title: 'Login Counts of New Users'
			};

			var chart = new google.visualization.PieChart(document.getElementById('login_count_chart_div'));

			chart.draw(data, options);

			// Display chart of tutorial states reached

			var data = new google.visualization.arrayToDataTable([
				['Tutorial State', 'Percentage of new users who reached the state'],
			";

	for ($i = 1; $i < $TUTORAL_STATES_COUNT; $i++)
	{
		if ($i > 1) echo ", ";
		echo "[\"".$i.": ".$TUTORIAL_STATE_ID[$i]."\", ".($tutorial_state_buckets[$i] * 100 / $num_new_users)."]";
	}

	echo "
			]);

			var options = {
				title: 'New Users Who Reached Each Tutorial State',
				width: 900,
				legend: { position: 'none' },
				chart: { title: 'New Users Who Reached Each Tutorial State',
								 subtitle: 'by percentage' },
				bars: 'horizontal', // Required for Material Bar Charts.
				axes: {
					x: {
						0: { side: 'top', label: 'Percentage'} // Top x-axis.
					}
				},
				bar: { groupWidth: \"90%\" }
			};

			var chart = new google.charts.Bar(document.getElementById('tutorial_state_chart_div'));
			chart.draw(data, options);

			// Display chart of whether new users have chatted during their latest session

			var data = google.visualization.arrayToDataTable([
          ['Chatted', 'New Users'],
        			";

	echo "['Yes', ".$chatted_buckets[$CHATTED_YES]."], ";
	echo "['No', ".$chatted_buckets[$CHATTED_NO]."]";

	echo "
			]);

			var options = {
				title: 'Whether New Users Chatted in Latest Session'
			};

			var chart = new google.visualization.PieChart(document.getElementById('chatted_chart_div'));

			chart.draw(data, options);

			// Display chart of what maps new users have viewed

			var data = google.visualization.arrayToDataTable([
          ['Viewed Maps', 'New Users'],
        			";

	echo "['Mainland, Homeland & Raid', ".$viewed_buckets[$VIEWED_MAINLAND_HOMELAND_RAID]."], ";
	echo "['Mainland & Homeland', ".$viewed_buckets[$VIEWED_MAINLAND_HOMELAND]."], ";
	echo "['Mainland', ".$viewed_buckets[$VIEWED_MAINLAND_ONLY]."]";

	echo "
			]);

			var options = {
				title: 'Maps Viewed by New Users'
			};

			var chart = new google.visualization.PieChart(document.getElementById('viewed_chart_div'));

			chart.draw(data, options);

			// Display chart of xp earned

			var data = google.visualization.arrayToDataTable([
          ['XP Earned', 'New Users'],
        			";

	echo "['0', ".$xp_buckets[$XP_0]."], ";
	echo "['1 to 20', ".$xp_buckets[$XP_1_TO_20]."], ";
	echo "['21 to 100', ".$xp_buckets[$XP_20_to_100]."], ";
	echo "['101 to 500', ".$xp_buckets[$XP_100_TO_500]."], ";
	echo "['501 to 1000', ".$xp_buckets[$XP_500_TO_1000]."], ";
	echo "['1001 to 5000', ".$xp_buckets[$XP_1000_TO_5000]."], ";
	echo "['5001 to 50,000', ".$xp_buckets[$XP_5000_TO_50000]."], ";
	echo "['50,001+', ".$xp_buckets[$XP_50000_PLUS]."]";

	echo "
			]);

			var options = {
				title: 'XP Earned by New Users'
			};

			var chart = new google.visualization.PieChart(document.getElementById('xp_chart_div'));

			chart.draw(data, options);

			// Display chart of platforms

			var data = google.visualization.arrayToDataTable([
          ['Platform', 'New Users'],
        			";

	echo "['Steam', ".$platform_buckets[$PLATFORM_STEAM]."], ";
	echo "['Windows', ".$platform_buckets[$PLATFORM_WINDOWS]."], ";
	echo "['Android', ".$platform_buckets[$PLATFORM_ANDROID]."], ";
	echo "['Unknown', ".$platform_buckets[$PLATFORM_UNKNOWN]."]";

	echo "
			]);

			var options = {
				title: 'Platforms of New Users'
			};

			var chart = new google.visualization.PieChart(document.getElementById('platform_chart_div'));

			chart.draw(data, options);

			// Display chart of new user creation time of day

			var data = google.visualization.arrayToDataTable([
          ['Creation Time', 'New Users'],
        			";

	echo "['12am to 3am', ".$tod_buckets[$TOD_0_TO_3]."], ";
	echo "['3am to 6am', ".$tod_buckets[$TOD_3_TO_6]."], ";
	echo "['6am to 9am', ".$tod_buckets[$TOD_6_TO_9]."], ";
	echo "['9am to 12pm', ".$tod_buckets[$TOD_9_TO_12]."], ";
	echo "['12pm to 3pm', ".$tod_buckets[$TOD_12_TO_15]."], ";
	echo "['3pm to 6pm', ".$tod_buckets[$TOD_15_TO_18]."], ";
	echo "['6pm to 9pm', ".$tod_buckets[$TOD_18_TO_21]."], ";
	echo "['9pm to 12am', ".$tod_buckets[$TOD_21_TO_24]."]";

	echo "
			]);

			var options = {
				title: 'Creation Time of New User Accounts'
			};

			var chart = new google.visualization.PieChart(document.getElementById('tod_chart_div'));

			chart.draw(data, options);
		}
	</script>
	";
}

?>

<script type="text/javascript">
	$('input[name="daterange"]').daterangepicker();
</script>
</body>