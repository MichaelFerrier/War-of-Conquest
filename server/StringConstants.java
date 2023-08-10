//
// War of Conquest Server
// Copyright (c) 2002-2023 Michael Ferrier, IronZog LLC
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//

package WOCServer;

import java.io.*;
import WOCServer.*;

public class StringConstants
{
	public static final String STR_COLON = ":";
	public static final String STR_SEMICOLON = ";";
	public static final String STR_COLON_QUOTE = ":\"";
	public static final String STR_QUOTE_SEMICOLON = "\";";
	public static final String STR_COLON_OPEN_BRACE = ":{";
	public static final String STR_CLOSE_BRACE = "}";
	public static final String STR_N_SEMICOLON = "N;";
	public static final String STR_i_COLON = "i:";
	public static final String STR_l_COLON = "l:";
	public static final String STR_d_COLON = "d:";
	public static final String STR_o_COLON = "o:";
	public static final String STR_s_COLON = "s:";
	public static final String STR_a_COLON = "a:";


  public static final String STR_PENALTY_EMAIL_TOP =
   "Dear War of Conquest player,\n\nThe following chat message was found to be in violation of the\nWar of Conquest rules of conduct:\n\n";

  public static final String STR_PENALTY_EMAIL_WARNING =
   "Further violations may result in your accounts being\ntemporarily disabled.\n\n\n";

  /*
  The big huge long rules section, like all rules was subject to change.
  So it has been decided that it is better to point players to the most
  current version of the rules/Code of Conduct/Terms and Conditions
  rather than trying to list them all here, which is icky
  */

  public static final String STR_PENALTY_EMAIL_RULES =
  "You can view all the Terms and Condtions for play, including the Code of Conduct, at the War of Conquest website.\nThe direct link is - https://warofconquest.com/conduct.htm";

  public static final String STR_PAGE_START ="<table class='content'>\n";
  public static final String STR_PAGE_END = "</table>";

  public static final String STR_NAV_BAR =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><script language='JavaScript'><!-- hide from older browser\n"
  +"      document.write(" + '\u0022' + "<img src='images/prize_nav_bar.gif' border='0'usemap='#prize_nav_bar'>" + '\u0022' + ")\n"
  +"      //--></script></td>\n"
  +"  </tr>\n";

  public static final String STR_GOALS_TOTAL_START =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'>\n"
  +"      <table name='total'>\n"
  +"        <tr>\n"
  +"          <td class='heading_lg'>Game Total Awarded:</td>\n"
  +"           <td class='text_lg'>$";

  public static final String STR_GOALS_TOTAL_END =
  "</td>\n"
  +"        </tr>\n"
  +"      </table>\n"
  +"    </td>\n"
  +"  </tr>\n";

  public static final String STR_NO_WINNERS =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'>None</td>\n"
  +"  </tr>\n";

  public static final String STR_MONTHLY_WINNERS =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div class='heading_med'>This Month's Top Prize Winners</div></td>\n"
  +"  </tr>\n";

  public static final String STR_DATA_TABLE_START =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'>\n"
  +"      <table class='data'>\n";

  public static final String STR_DATA_ROW_START =
  "        <tr>\n"
  +"          <td class='name'>";

  public static final String STR_DATA_ROW_MIDDLE =
  "</td>\n"
  +"          <td class='amount'>";

  public static final String STR_DATA_ROW_END =
  "</td>\n"
  +"        </tr>\n";

  public static final String STR_DATA_TABLE_END =
  "      </table>\n"
  +"    </td>\n"
  +"  </tr>\n";

  public static final String STR_SPACER_ROW =
  "   <tr>\n"
  +"    <td colspan='2' class='spacer_row'>&nbsp;</td>\n"
  +"  </tr>\n";
  public static final String STR_ROW_BEGIN =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div>";

  public static final String STR_ROW_END =
  "</div></td>\n"
  +"  </tr>\n";

  public static final String STR_HEADING_ROW_BEGIN =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div class='heading_reg'>";

  public static final String STR_HEADING_ROW_END =
  "</div></td>\n"
  +"  </tr>\n";

  public static final String STR_ALLTIME_WINNERS =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div class='heading_med'>All-Time Top Winners</div></td>\n"
  +"  </tr>\n";

  public static final String STR_MONTHLY_NATIONS =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div class='heading_med'>This Month's Top Nations</div></td>\n"
  +"  </tr>\n";

  public static final String STR_MONTHLY_PLAYERS =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div class='heading_med'>This Month's Top Players</div></td>\n"
  +"  </tr>\n";


  public static final String STR_ALLTIME_NATIONS =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div class='heading_med'>All-Time Top Scoring Nations</div></td>\n"
  +"  </tr>\n";

  public static final String STR_ALLTIME_PLAYERS =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div class='heading_med'>All-Time Top Scoring Players</div></td>\n"
  +"  </tr>\n";

  public static final String STR_CURRENT_WINNERS =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div class='heading_med'>Most Recent Winners</div></td>\n"
  +"  </tr>\n";

  public static final String STR_TOP_ORB_WINNERS =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div class='heading_med'>Top Winners</div></td>\n"
  +"  </tr>\n";

  public static final String STR_ORB_NAME_ROW =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'><div class='heading_med_alt'>";

  public static final String STR_ORB_TABLE =
  "  <tr>\n"
  +"    <td class='left_column'>&nbsp;</td>\n"
  +"    <td class='right_column'>\n"
  +"      <table name='orb'>\n"
  +"        <tr>\n"
  +"          <td class='heading_reg'>Located At: </td>\n"
  +"          <td>";

  public static final String STR_ORB_ROW_END =
  "</td>\n"
  +"        </tr>\n";

  public static final String STR_ORB_TOTAL_ROW =
  "        <tr>\n"
  +"          <td class='heading_reg'>Total Award: </td>\n"
  +"          <td>$";

  public static final String STR_ORB_SPAN_ROW =
  "  <tr>\n"
  +"   <td colspan='2'>";

  public static final String STR_ORB_SPAN_ROW_END =
  "<br></td>\n"
  +"  </tr>\n";

  public static final String STR_ORB_TABLE_END =
  "       </table>\n"
  +"    </td>\n"
  +"  </tr>\n";

	public static final String STR_XML_EXT = ".xml";
	public static final String STR_ORB_HISTORY = "orb_history";
	public static final String STR_ORB_HISTORY_MONTHLY = "orb_history_monthly";
	public static final String STR_RANKS_COMBINED = "ranks_combined";
	public static final String STR_RANKS_NATION_XP = "ranks_nation_xp";
	public static final String STR_RANKS_NATION_XP_MONTHLY = "ranks_nation_xp_monthly";
	public static final String STR_RANKS_USER_XP = "ranks_user_xp";
	public static final String STR_RANKS_USER_XP_MONTHLY = "ranks_user_xp_monthly";
	public static final String STR_RANKS_USER_FOLLOWERS = "ranks_user_followers";
	public static final String STR_RANKS_USER_FOLLOWERS_MONTHLY = "ranks_user_followers_monthly";
	public static final String STR_RANKS_NATION_WINNINGS = "ranks_nation_winnings";
	public static final String STR_RANKS_NATION_WINNINGS_MONTHLY = "ranks_nation_winnings_monthly";
	public static final String STR_RANKS_NATION_LATEST_TOURNAMENT = "ranks_nation_latest_tournament";
	public static final String STR_RANKS_NATION_TOURNAMENT_TROPHIES = "ranks_nation_tournament_trophies";
	public static final String STR_RANKS_NATION_TOURNAMENT_TROPHIES_MONTHLY = "ranks_nation_tournament_trophies_monthly";
	public static final String STR_RANKS_NATION_LEVEL = "ranks_nation_level";
	public static final String STR_RANKS_NATION_REBIRTHS = "ranks_nation_rebirths";
	public static final String STR_RANKS_NATION_QUESTS = "ranks_nation_quests";
	public static final String STR_RANKS_NATION_QUESTS_MONTHLY = "ranks_nation_quests_monthly";
	public static final String STR_RANKS_NATION_ENERGY_DONATED = "ranks_nation_energy_donated";
	public static final String STR_RANKS_NATION_ENERGY_DONATED_MONTHLY = "ranks_nation_energy_donated_monthly";
	public static final String STR_RANKS_NATION_MANPOWER_DONATED = "ranks_nation_manpower_donated";
	public static final String STR_RANKS_NATION_MANPOWER_DONATED_MONTHLY = "ranks_nation_manpower_donated_monthly";
	public static final String STR_RANKS_NATION_AREA = "ranks_nation_area";
	public static final String STR_RANKS_NATION_AREA_MONTHLY = "ranks_nation_area_monthly";
	public static final String STR_RANKS_NATION_CAPTURES = "ranks_nation_captures";
	public static final String STR_RANKS_NATION_CAPTURES_MONTHLY = "ranks_nation_captures_monthly";
	public static final String STR_RANKS_TOURNAMENT_CURRENT = "ranks_nation_tournament_current";
	public static final String STR_RANKS_NATION_MEDALS = "ranks_nation_medals";
	public static final String STR_RANKS_NATION_MEDALS_MONTHLY = "ranks_nation_medals_monthly";
	public static final String STR_RANKS_NATION_RAID_EARNINGS = "ranks_nation_raid_earnings";
	public static final String STR_RANKS_NATION_RAID_EARNINGS_MONTHLY = "ranks_nation_raid_earnings_monthly";
	public static final String STR_RANKS_NATION_ORB_SHARD_EARNINGS = "ranks_nation_orb_shard_earnings";
	public static final String STR_RANKS_NATION_ORB_SHARD_EARNINGS_MONTHLY = "ranks_nation_orb_shard_earnings_monthly";

	public static final String STR_XML_START =
	"<?xml version=\"1.0\"?>\n<lists>\n";

	public static final String STR_XML_END =
	"</lists>";

	public static final String STR_XML_LIST_START =
	"	<list>\n";

	public static final String STR_XML_LIST_END =
	"	</list>\n";

	public static final String STR_XML_RANKS_START =
	"		<ranks>\n";

	public static final String STR_XML_RANKS_END =
	"		</ranks>\n";

	public static final String STR_XML_ID_START =
	"		<ID>";

	public static final String STR_XML_ID_END =
	"</ID>\n";

	public static final String STR_XML_TOTAL_START =
	"		<total>";

	public static final String STR_XML_TOTAL_END =
	"</total>\n";

	public static final String STR_XML_RANK_LINE_1 =
	"			<rank ID=\"";

	public static final String STR_XML_RANK_LINE_2 =
	"\" name=\"";

	public static final String STR_XML_RANK_LINE_3 =
	"\" amount=\"";

	public static final String STR_XML_RANK_LINE_3A =
	"\" active=\"";

	public static final String STR_XML_RANK_LINE_4 =
	"\"/>\n";
}
