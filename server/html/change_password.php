<?php

//error_reporting(E_ALL);
ini_set('display_errors', 1);

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

$username = isset($_GET["username"]) ? $_GET["username"] : "";
$password = isset($_GET["password"]) ? $_GET["password"] : "";
$new_password = isset($_GET["new_password"]) ? $_GET["new_password"] : "";

//WriteToLog("username: $username, password: $password, new_password: $new_password"); // TESTING

// Query database for user data
$query_string = "SELECT * FROM Accounts WHERE username='".mysql_real_escape_string($username)."'";

$result = mysql_query($query_string);
$row = mysql_fetch_array($result);

//WriteToLog("original passhash: ".$row["passhash"]); // TESTING

if ($row == false) 
{
  echo "1\nNo user found with username '$username'.";
  return;
}

if ($row["passhash"] != hash('sha256', $password))
{
	echo "2\nPassword doesn't match.";
	return;
}

if ((strlen($new_password) < 6) || (strlen($new_password) > 20)) 
{
	echo "3\nYour new password must be between 6 and 20 characters long.";
	return;
}

if (strpos($new_password, " ") !== false)
{
	echo "3\nYour new password cannot contain spaces.";
	return;
}

$new_passhash = hash('sha256', $new_password);

$query_string = "UPDATE Accounts SET passhash='$new_passhash' WHERE username='".mysql_real_escape_string($username)."'";
$result = mysql_query($query_string);

//// TESTING
//WriteToLog("query_string: $query_string");
//WriteToLog("new_passhash: $new_passhash, result: $result,  mysql_affected_rows(): ". mysql_affected_rows()); // TESTING
//$query_string = "SELECT * FROM Accounts WHERE username='".mysql_real_escape_string($username)."'";
//$result = mysql_query($query_string);
//$row = mysql_fetch_array($result);
//WriteToLog("new passhash: ".$row["passhash"]); // TESTING

// If there was an error, return error message.
if ($result == 0)
{
	echo "4\nCould not update password.";
	return;
}

echo "0\nPassword changed.";

function WriteToLog($_text)
{
	$handle = fopen("log.txt", "a");
	fwrite($handle, $_text."\r\n");
}

?>