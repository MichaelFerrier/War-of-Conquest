<?php

//error_reporting(E_ALL);
ini_set('display_errors', 1);

if (isset($_GET["email"]) === false) {
	echo "Invalid parameters.";
	exit;
}

if (isset($_GET["password"]) === false) {
	echo "Invalid parameters.";
	exit;
}

if (isset($_GET["key"]) === false) {
	echo "Invalid parameters.";
	exit;
}

$email = isset($_GET["email"]) ? $_GET["email"] : "";
$password = isset($_GET["password"]) ? $_GET["password"] : "";
$key = isset($_GET["key"]) ? $_GET["key"] : "";

//WriteToLog("username: $username, password: $password, key: $key"); // TESTING

// Check key
if ($key != "k79cjwFgwU")
{
	echo "invalid";
	return;
}

require_once 'player_account_db_include.php';

// Connect to MySQL and select the database
$db = mysql_connect($player_account_db_host, $player_account_db_username, $player_account_db_password);

if (!$db) {
    die('Could not connect: ' . mysql_error());
}

$select = mysql_select_db($player_account_db_database, $db);

if (!$select) {
    die('Could not select: ' . mysql_error());
}

// Query database for user data
$query_string = "SELECT * FROM Accounts WHERE email='".mysql_real_escape_string($email)."' AND passhash='".hash('sha256', $password)."'";

$result = mysql_query($query_string);

$num_results = mysql_num_rows($result);

// Compile a list of all usernames associated with this email and password, and determine if any of them are banned.
$usernames = "";
$banned = false;
$username_count = 0;
while ($row = mysql_fetch_assoc($result)) 
{
	$usernames .= ("\n".$row['username']."\t".$row['ID']);
	$username_count++;
	if ($row['game_ban_end_time'] > time()) {
		$banned = true;
	}
}

if ($banned)
{
	echo "-1";
}
else
{
	echo $username_count;
	echo $usernames;
}

function WriteToLog($_text)
{
	$handle = fopen("log.txt", "a");
	fwrite($handle, $_text."\r\n");
}

?>