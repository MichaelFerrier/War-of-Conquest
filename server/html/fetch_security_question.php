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

// Query database for user data
$query_string = "SELECT * FROM Accounts WHERE username='".mysql_real_escape_string($username)."'";

$result = mysql_query($query_string);
$row = mysql_fetch_array($result);

if ($row == false) 
{
  echo "1\nNo user found with username '$username'.";
  return;
}

echo "0\n".$row["security_question"];

?>