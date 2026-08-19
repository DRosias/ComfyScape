package content.global.skill.farming.timers

import core.tools.*
import core.game.system.timer.*
import org.json.simple.*
import core.game.node.entity.Entity
import core.game.node.entity.player.Player
import content.global.skill.farming.*

class CropGrowth : PersistTimer (50, "farming:crops", isSoft = true) { 
    private val patchMap = HashMap<FarmingPatch, Patch>()
    lateinit var player: Player

    override fun onRegister (entity: Entity) {
        player = (entity as? Player)!!
        runOfflineCatchupLogic()
    }

    override fun run (entity: Entity) : Boolean {
        var removeList = ArrayList<FarmingPatch>()
        for((fp,patch) in patchMap){
            if(patch.getCurrentState() in 1..3 && patch.nextGrowth == 0L){
                patch.nextGrowth = System.currentTimeMillis() + 60000
                continue
            }

            val now = System.currentTimeMillis()
            while(patch.nextGrowth <= now && !patch.isDead && !patch.isChoppedFruitTree()){
                patch.nextGrowth += patch.getStageGrowthMillis()
                patch.update()
            }

            if (patch.getCurrentState() == 0)
                removeList.add(fp)
        }
        removeList.forEach { patchMap.remove(it) }
        removeList.clear()
        return patchMap.isNotEmpty()
    }

    private fun runOfflineCatchupLogic() {
        for ((_, patch) in patchMap) {
            val type = patch.patch.type
            val shouldPlayCatchup = !patch.isGrown() || (type == PatchType.BUSH_PATCH && patch.getFruitOrBerryCount() < 4) || (type == PatchType.FRUIT_TREE_PATCH && patch.getFruitOrBerryCount() < 6) || (patch.plantable == Plantable.WILLOW_SAPLING && patch.harvestAmt < 6)
            if (shouldPlayCatchup && !patch.isDead && !patch.isChoppedFruitTree()) {
                var stagesToSimulate = if (!patch.isGrown()) {
                    if (patch.isWeedy() || patch.isEmptyAndWeeded()) patch.currentGrowthStage % 4
                    else patch.plantable!!.stages - patch.currentGrowthStage
                } else 0

                if (patch.plantable != null) {
                    if (type == PatchType.BUSH_PATCH)
                        stagesToSimulate += Math.min(4, 4 - patch.getFruitOrBerryCount())
                    if (type == PatchType.FRUIT_TREE_PATCH)
                        stagesToSimulate += Math.min(6, 6 - patch.getFruitOrBerryCount())
                    if (patch.plantable == Plantable.WILLOW_SAPLING)
                        stagesToSimulate += Math.min(6, 6 - patch.harvestAmt)
                }

                val nowTime = System.currentTimeMillis()
                var simulatedTime = patch.nextGrowth

                while (simulatedTime < nowTime && stagesToSimulate-- > 0 && !patch.isDead) {
                    val timeToIncrement = patch.getStageGrowthMillis()
                    patch.update()
                    simulatedTime += timeToIncrement
                }
                patch.nextGrowth = simulatedTime
            }
        }
    }

    fun getPatch(patch: FarmingPatch, addPatch: Boolean ): Patch {
        return patchMap[patch] ?: (Patch(player,patch).also { if (addPatch) patchMap[patch] = it })
    }

    fun getPatches(): MutableCollection<Patch>{
        return patchMap.values
    }

    override fun save (root: JSONObject, entity: Entity) {
        val patches = JSONArray()
        for((key,patch) in patchMap){
            val p = JSONObject()
            p.put("patch-ordinal",key.ordinal)
            p.put("patch-plantable-ordinal",patch.plantable?.ordinal ?: -1)
            p.put("patch-watered",patch.isWatered)
            p.put("patch-diseased",patch.isDiseased)
            p.put("patch-dead",patch.isDead)
            p.put("patch-stage",patch.currentGrowthStage)
            p.put("patch-state",patch.getCurrentState())
            p.put("patch-nextGrowth",patch.nextGrowth)
            p.put("patch-harvestAmt",patch.harvestAmt)
            p.put("patch-checkHealth",patch.isCheckHealth)
            p.put("patch-compost",patch.compost.ordinal)
            p.put("patch-paidprot",patch.protectionPaid)
            p.put("patch-croplives", patch.cropLives)
            patches.add(p)
        }
        root["patches"] = patches
    }

    override fun parse (root: JSONObject, entity: Entity) {
        val data = root["patches"] as JSONArray
        for(d in data){
            val p = d as JSONObject
            val patchOrdinal = p["patch-ordinal"].toString().toInt()
            val patchPlantableOrdinal = p["patch-plantable-ordinal"].toString().toInt()
            val patchWatered = p["patch-watered"] as Boolean
            val patchDiseased = p["patch-diseased"] as Boolean
            val patchDead = p["patch-dead"] as Boolean
            val patchStage = p["patch-stage"].toString().toInt()
            val nextGrowth = p["patch-nextGrowth"].toString().toLong()
            val harvestAmt = (p["patch-harvestAmt"] ?: 0).toString().toInt()
            val checkHealth = p["patch-checkHealth"] as Boolean
            val savedState = p["patch-state"].toString().toInt()
            val compostOrdinal = p["patch-compost"].toString().toInt()
            val protectionPaid = p["patch-paidprot"] as Boolean
            val cropLives = if(p["patch-croplives"] != null) p["patch-croplives"].toString().toInt() else 3
            val fPatch = FarmingPatch.values()[patchOrdinal]
            val plantable = if(patchPlantableOrdinal != -1) Plantable.values()[patchPlantableOrdinal] else null
            val patch = Patch((entity as? Player)!!,fPatch,plantable,patchStage,patchDiseased,patchDead,patchWatered,nextGrowth,harvestAmt,checkHealth)

            patch.cropLives = cropLives
            patch.compost = CompostType.values()[compostOrdinal]
            patch.protectionPaid = protectionPaid
            patch.setCurrentState(savedState)

            if((savedState - (patch?.plantable?.value ?: 0)) > patch.currentGrowthStage){
                patch.setCurrentState(savedState)
            } else {
                patch.setCurrentState((patch.plantable?.value ?: 0) + patch.currentGrowthStage)
            }

            if(patchMap[fPatch] == null) {
                patchMap[fPatch] = patch
            }
        }
    }
}
