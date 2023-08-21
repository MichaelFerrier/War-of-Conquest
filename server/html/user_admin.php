<?php

//error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'player_account_db_include.php';

// Initialize error string
$error = "";

// Connect to MySQL and select the database
$db = mysql_connect($player_account_db_host, $player_account_db_username, $player_account_db_password);

if (!$db) {
    die('Could not connect: ' . mysql_error());
}

$select = mysql_select_db($player_account_db_database, $db);

if (!$select) {
    die('Could not select: ' . mysql_error());
}

/// ONE TIME ONLY!!
//$query_string = "UPDATE lm_user_record SET license=1 WHERE num_sites_purchased=0";
//$result = mysql_query($query_string);
//$query_string = "UPDATE lm_user_record SET license=3 WHERE num_sites_purchased>0";
//$result = mysql_query($query_string);

$username = isset($_POST["username"]) ? $_POST["username"] : "";
$username_lower = strtolower($username);

$userID = isset($_POST["userID"]) ? $_POST["userID"] : "";

echo"
<html>
<head>
<title>User Admin</title>
<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">
<style>
.labelcell {
    background-color: #FFEEBB;
    border-collapse: collapse;
    padding:4px;
    font-family: Tahoma, Verdana, Arial, Helvetica, sans-serif;
    font-size: 13px;
    color: #000000;
    text-align:right;
    vertical-align:text-middle;
}
.valuecell {
    background-color: #FFEEBB;
    border-collapse: collapse;
    padding:4px;
    font-family: Tahoma, Verdana, Arial, Helvetica, sans-serif;
    font-size: 13px;
    color: #000000;
    vertical-align:text-middle;
}
</style>
</head>

<body bgcolor=\"#FFFFFF\">

<form name=\"query_form\" action=\"user_admin.php\" method=\"post\">
Admin Password:</br>
<input type=\"text\" name=\"admin_password\" size=\"68\" value=\"$admin_password\"></br>
Enter Username:</br>
<input type=\"text\" name=\"username\" size=\"68\" value=\"$username\"></br>
or Enter User ID:</br>
<input type=\"text\" name=\"userID\" size=\"68\" value=\"$userID\"></br>
<input type=\"submit\" name=\"Submit\" value=\"Submit\">
</form>
<br>
";

if ((!isset($_POST["admin_password"])) || ($_POST["admin_password"] != $admin_password) || (($username == "") && ($userID == "")))
{
  echo "</body></html>";
  return;
}

/*
if (isset($_POST["store_data"]) && ($_POST["store_data"] == 1))
{
  $cur_time = time();

  $query_string = "UPDATE lm_user_record SET num_sites_purchased='".$_POST["num_sites_purchased"]."', trial_start_time='".$_POST["trial_start_time"]."', affiliate_id='".$_POST["affiliate_id"]."', revenue='".$_POST["revenue"]."', survey_sent='".$_POST["survey_sent"]."', email='$email', email_lower='$email_lower', password='".$_POST["password"]."', license='".$_POST["license"]."', cur_period='".$_POST["cur_period"]."', cur_period_sites='".stripslashes($_POST["cur_period_sites"])."' WHERE user_id='".$_POST["user_id"]."'";
  $result = mysql_query($query_string);

  // If there was an error, return error message.
  if ($result == 0)
  {
    echo "Error: ".mysql_error();
    return;
  }

  echo"
  User data updated.<br>
  <br>
  ";
}
*/

// Query database for user data
if ($username != "") {
	$query_string = "SELECT * FROM Accounts WHERE username='".mysql_real_escape_string($username)."'";
} else {
	$query_string = "SELECT * FROM Accounts WHERE ID='".mysql_real_escape_string($userID)."'";
}

$result = mysql_query($query_string);
$row = mysql_fetch_array($result);

if ($row == false) 
{
  echo "No user found with username '$username' or ID '$userID'.</body></html>";
  return;
}

echo"
<form name=\"user_form\" action=\"user_admin.php\" method=\"post\">
<input type='hidden' name=\"store_data\" value=\"1\"/>
<table width=\"500\">
  <tr> 
    <td class=labelcell>ID</td>
    <td class=valuecell>".$row["ID"]."</td>
    <input type=\"hidden\" name=\"user_id\" value=\"".$row["ID"]."\">
  </tr>
  <tr> 
    <td class=labelcell>username</td>
    <td class=valuecell><input type=\"text\" name=\"email_lower\" size=40 value=\"".$row["username"]."\"></td>
  </tr>
  <tr> 
    <td class=labelcell>email</td>
    <td class=valuecell><input type=\"text\" name=\"email\" size=40 value=\"".$row["email"]."\"></td>
  </tr>
  <tr> 
    <td class=labelcell>security_question</td>
    <td class=valuecell><input type=\"text\" name=\"password\" size=40 value=\"".$row["security_question"]."\"></td>
  </tr>
  <tr> 
    <td class=labelcell>security_answer</td>
    <td class=valuecell><input type=\"text\" name=\"license\" size=40 value=\"".$row["security_answer"]."\"></td>
  </tr>
  <tr> 
    <td class=labelcell>woc2_serverID</td>
    <td class=valuecell><input type=\"text\" name=\"num_sites_purchased\" size=40 value=\"".$row["woc2_serverID"]."\"></td>
  </tr>
  <tr> 
    <td class=labelcell>woc2_clientID</td>
    <td class=valuecell><input type=\"text\" name=\"cur_period\" size=40 value=\"".$row["woc2_clientID"]."\"></td>
  </tr>
</table>
<!--<input type=\"submit\" name=\"Submit\" value=\"Submit Changes\">-->
</form>
  ";

?>