/* Generated file, do not edit */

#ifndef CXXTEST_RUNNING
#define CXXTEST_RUNNING
#endif

#include <fstream>
#define _CXXTEST_HAVE_EH
#define _CXXTEST_ABORT_TEST_ON_FAIL
#include <cxxtest/TestListener.h>
#include <cxxtest/TestTracker.h>
#include <cxxtest/TestRunner.h>
#include <cxxtest/RealDescriptions.h>
#include <cxxtest/TestMain.h>
#include <cxxtest/XUnitPrinter.h>

int main( int argc, char *argv[] ) {
 int status;
    std::ofstream ofstr("cxxtest-report-compute-pi-test.xml");
    CxxTest::XUnitPrinter tmp(ofstr);
    CxxTest::RealWorldDescription::_worldName = "cxxtest";
    status = CxxTest::Main< CxxTest::XUnitPrinter >( tmp, argc, argv );
    return status;
}
bool suite_ComputePiTest_init = false;
#include "${testDir}\compute-pi-test\compute-pi-test.h"

static ComputePiTest suite_ComputePiTest;

static CxxTest::List Tests_ComputePiTest = { 0, 0 };
CxxTest::StaticSuiteDescription suiteDescription_ComputePiTest( "${testDir.unixSlash}/compute-pi-test/compute-pi-test.h", 5, "ComputePiTest", suite_ComputePiTest, Tests_ComputePiTest );

static class TestDescription_suite_ComputePiTest_testComputePi : public CxxTest::RealTestDescription {
public:
 TestDescription_suite_ComputePiTest_testComputePi() : CxxTest::RealTestDescription( Tests_ComputePiTest, suiteDescription_ComputePiTest, 8, "testComputePi" ) {}
 void runTest() { suite_ComputePiTest.testComputePi(); }
} testDescription_suite_ComputePiTest_testComputePi;

#include <cxxtest/Root.cpp>
const char* CxxTest::RealWorldDescription::_worldName = "cxxtest";
