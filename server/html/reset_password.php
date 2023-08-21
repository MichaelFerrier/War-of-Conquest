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
$answer = isset($_GET["answer"]) ? $_GET["answer"] : "";

// Query database for user data
$query_string = "SELECT * FROM Accounts WHERE username='".mysql_real_escape_string($username)."'";

$result = mysql_query($query_string);
$row = mysql_fetch_array($result);

if ($row == false) 
{
  echo "2\nNo user found with username '$username'.";
  return;
}

if (strtolower($row["security_answer"]) != strtolower($answer))
{
	echo "1\nAnswer doesn't match account.";
	return;
}

// Generate new random password.
$length = 8;
$chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$?";
$new_password = substr( str_shuffle( $chars ), 0, $length );
$new_password_hash = hash('sha256', $new_password);

$query_string = "UPDATE Accounts SET passhash='$new_password_hash' WHERE username='".mysql_real_escape_string($username)."'";
$result = mysql_query($query_string);

// If there was an error, return error message.
if ($result == 0)
{
	echo "3\nCould not update password.";
	return;
}

echo "0\n".$row["email"]."\n".$new_password."\nPassword changed.";

?>