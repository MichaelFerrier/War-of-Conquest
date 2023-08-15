using UnityEngine;
using System.Collections;

public class BuildPrefabs : MonoBehaviour
{
    public GameObject construction;

    public GameObject wall1Post;
    public GameObject wall1Length;
    public GameObject wall2Post;
    public GameObject wall2Length;
    public GameObject wall3Post;
    public GameObject wall3Length;

    public GameObject plasmaScreen1Post;
    public GameObject plasmaScreen1Length;
    public GameObject plasmaScreen2Post;
    public GameObject plasmaScreen2Length;
    public GameObject plasmaScreen3Post;
    public GameObject plasmaScreen3Length;

    public GameObject voidDam1Post;
    public GameObject voidDam1Length;
    public GameObject voidDam2Post;
    public GameObject voidDam2Length;
    public GameObject voidDam3Post;
    public GameObject voidDam3Length;

    public GameObject hedge1Post;
    public GameObject hedge1Length;
    public GameObject hedge2Post;
    public GameObject hedge2Length;
    public GameObject hedge3Post;
    public GameObject hedge3Length;

    public GameObject stranglingVines1Post;
    public GameObject stranglingVines1Length;
    public GameObject stranglingVines2Post;
    public GameObject stranglingVines2Length;
    public GameObject stranglingVines3Post;
    public GameObject stranglingVines3Length;

    public GameObject rootBarricade1Post;
    public GameObject rootBarricade1Length;
    public GameObject rootBarricade2Post;
    public GameObject rootBarricade2Length;
    public GameObject rootBarricade3Post;
    public GameObject rootBarricade3Length;

    public GameObject telekineticBlock1Post;
    public GameObject telekineticBlock1Length;
    public GameObject telekineticBlock2Post;
    public GameObject telekineticBlock2Length;
    public GameObject telekineticBlock3Post;
    public GameObject telekineticBlock3Length;

    public GameObject pyralisade1Post;
    public GameObject pyralisade1Length;
    public GameObject pyralisade2Post;
    public GameObject pyralisade2Length;
    public GameObject pyralisade3Post;
    public GameObject pyralisade3Length;

    public GameObject ectochasm1Post;
    public GameObject ectochasm1Length;
    public GameObject ectochasm2Post;
    public GameObject ectochasm2Length;
    public GameObject ectochasm3Post;
    public GameObject ectochasm3Length;

    public GameObject cannon1;
    public GameObject cannon2;
    public GameObject cannon3;
    public GameObject artillery1;
    public GameObject artillery2;
    public GameObject artillery3;
    public GameObject rocketLauncher1;
    public GameObject rocketLauncher2;
    public GameObject rocketLauncher3;

    public GameObject telekineticProjector1;
    public GameObject telekineticProjector2;
    public GameObject telekineticProjector3;
    public GameObject keraunocon1;
    public GameObject keraunocon2;
    public GameObject keraunocon3;
    public GameObject pyroclasm1;
    public GameObject pyroclasm2;
    public GameObject pyroclasm3;

    public GameObject pestilenceLauncher1;
    public GameObject pestilenceLauncher2;
    public GameObject pestilenceLauncher3;
    public GameObject toxicMistLauncher1;
    public GameObject toxicMistLauncher2;
    public GameObject toxicMistLauncher3;
    public GameObject pathogenSporeLauncher1;
    public GameObject pathogenSporeLauncher2;
    public GameObject pathogenSporeLauncher3;
    public GameObject nanobotSwarmBase1;
    public GameObject nanobotSwarmBase2;
    public GameObject nanobotSwarmBase3;

    public GameObject radioTower1;
    public GameObject radioTower2;
    public GameObject radioTower3;
    public GameObject satComCommand1;
    public GameObject satComCommand2;
    public GameObject satComCommand3;
    public GameObject autonomousWarBase1;
    public GameObject autonomousWarBase2;
    public GameObject autonomousWarBase3;

    public GameObject deadHand1;
    public GameObject deadHand2;
    public GameObject deadHand3;
    public GameObject geographicWipe1;
    public GameObject geographicWipe2;
    public GameObject geographicWipe3;

    public GameObject toxicChemicalDump1;
    public GameObject toxicChemicalDump2;
    public GameObject toxicChemicalDump3;
    public GameObject supervirusContagion1;
    public GameObject supervirusContagion2;
    public GameObject supervirusContagion3;
    public GameObject hypnoticInducer1;
    public GameObject hypnoticInducer2;
    public GameObject hypnoticInducer3;
    public GameObject templeOfZothOmmog1;
    public GameObject templeOfZothOmmog2;
    public GameObject templeOfZothOmmog3;

    public GameObject ectoRay1;
    public GameObject ectoRay2;
    public GameObject ectoRay3;
    public GameObject djinnPortal1;
    public GameObject djinnPortal2;
    public GameObject djinnPortal3;

    public GameObject guidedMissileStation1;
    public GameObject guidedMissileStation2;
    public GameObject guidedMissileStation3;

    public GameObject treeSummoner1;
    public GameObject treeSummoner2;
    public GameObject treeSummoner3;
    public GameObject rootsOfDespair1;
    public GameObject rootsOfDespair2;
    public GameObject rootsOfDespair3;

    public GameObject brainSweeper1;
    public GameObject brainSweeper2;
    public GameObject brainSweeper3;

    public GameObject phantasmicThreat1;
    public GameObject phantasmicThreat2;
    public GameObject phantasmicThreat3;

    public GameObject fuelTank1;
    public GameObject fuelTank2;
    public GameObject fuelTank3;
    public GameObject agroColony1;
    public GameObject agroColony2;
    public GameObject agroColony3;
    public GameObject hydroponicGarden1;
    public GameObject hydroponicGarden2;
    public GameObject hydroponicGarden3;
    public GameObject energyVortex1;
    public GameObject energyVortex2;
    public GameObject energyVortex3;
    public GameObject earthChakra1;
    public GameObject earthChakra2;
    public GameObject earthChakra3;

    public GameObject shardRed;
    public GameObject shardGreen;
    public GameObject shardBlue;

    // Lasting wipes
    public GameObject wipeChemical;
    public GameObject wipeSupervirus;
    public GameObject wipeHypnotic;
    public GameObject wipeTemple;

    // Effects
    public GameObject iceWave;
    public GameObject fireball;
    public GameObject deadHand1Nuke;
    public GameObject deadHand2Nuke;
    public GameObject deadHand3Nuke;
    public GameObject geoWipeFlame;
    public GameObject treeSummonerFX;
    public GameObject rootsDesp1FX;
    public GameObject rootsDesp2FX;
    public GameObject rootsDesp3FX;
    public GameObject brainsweeperFX;

    public GameObject toxChemDump1Fog;
    public GameObject toxChemDump2Fog;
    public GameObject toxChemDump3Fog;
    public GameObject supVirCont1Fog;
    public GameObject supVirCont2Fog;
    public GameObject supVirCont3Fog;
    public GameObject hypnoticInd1FX;
    public GameObject hypnoticInd2FX;
    public GameObject hypnoticInd3FX;
    public GameObject temple1FX;
    public GameObject temple2FX;
    public GameObject temple3FX;

    public GameObject radTow1Raid;
    public GameObject radTow2Raid;
    public GameObject radTow3Raid;
    public GameObject satCom1Raid;
    public GameObject satCom2Raid;
    public GameObject satCom3Raid;
    public GameObject warBase1Raid;
    public GameObject warBase2Raid;
    public GameObject warBase3Raid;

    // Battle effects
    public GameObject radioactiveFalloutFX;
    public GameObject contagionFX;
    public GameObject psychicShockwaveFX;
    public GameObject guerrillaWarfareFX;
    public GameObject battlePlagueFX;

    // Landscape objects
    public GameObject ironDeposit;
    public GameObject ancientStarshipWreckage;
    public GameObject uniqueEcosystem;
    public GameObject cryptidColony;
    public GameObject henge;
    public GameObject oracle;
    public GameObject oilDeposit;
    public GameObject uraniumDeposit;
    public GameObject nickelDeposit;
    public GameObject geothermalVent;
    public GameObject leyLines;
    public GameObject quartzDeposit;
    public GameObject graveOfAnAncientGod;
    public GameObject freshWater;
    public GameObject fertileSoil;
    public GameObject researchFacility;
    public GameObject alchemistsLair;

    // Orbs
    public GameObject orbOfNoontide;
    public GameObject orbOfShadow;
    public GameObject orbOfTheVoid;
    public GameObject orbOfDestiny;
    public GameObject orbOfFire;

    // Discovery icon
    public GameObject discoveryIcon;

    public static BuildPrefabs instance;

	public BuildPrefabs()
    {
        instance = this;
	}
}
