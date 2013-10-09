#pragma once
#include <cxxtest/TestSuite.h>
#include "compute-pi.h"

class ComputePiTest : public CxxTest::TestSuite
{
public:
    void testComputePi()
    {
        double pi = computePI(COMPUTE_PI_ITERATIONS);
        TS_ASSERT_DELTA(pi, 3.1415, 0.01);
    }
};
