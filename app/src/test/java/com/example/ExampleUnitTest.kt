package com.example

import com.example.engine.ProfileMerger
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSpatialClustering_mergesOverlappingBoxes() {
        val box1 = ProfileMerger.RectBox(left = 10, top = 10, right = 100, bottom = 100)
        val box2 = ProfileMerger.RectBox(left = 50, top = 50, right = 150, bottom = 150)

        val merged = ProfileMerger.clusterRectangles(listOf(box1, box2), threshold = 25)

        assertEquals(1, merged.size)
        assertEquals(10, merged[0].left)
        assertEquals(10, merged[0].top)
        assertEquals(150, merged[0].right)
        assertEquals(150, merged[0].bottom)
    }

    @Test
    fun testSpatialClustering_mergesProximateBoxesWithinThreshold() {
        // Gap of 10px between right of box1 (100) and left of box2 (110) <= threshold 25px
        val box1 = ProfileMerger.RectBox(left = 10, top = 10, right = 100, bottom = 100)
        val box2 = ProfileMerger.RectBox(left = 110, top = 20, right = 200, bottom = 100)

        val merged = ProfileMerger.clusterRectangles(listOf(box1, box2), threshold = 25)

        assertEquals(1, merged.size)
        assertEquals(10, merged[0].left)
        assertEquals(10, merged[0].top)
        assertEquals(200, merged[0].right)
        assertEquals(100, merged[0].bottom)
    }

    @Test
    fun testSpatialClustering_keepsDistantBoxesSeparate() {
        val box1 = ProfileMerger.RectBox(left = 10, top = 10, right = 50, bottom = 50)
        val box2 = ProfileMerger.RectBox(left = 200, top = 200, right = 250, bottom = 250)

        val merged = ProfileMerger.clusterRectangles(listOf(box1, box2), threshold = 25)

        assertEquals(2, merged.size)
    }
}
