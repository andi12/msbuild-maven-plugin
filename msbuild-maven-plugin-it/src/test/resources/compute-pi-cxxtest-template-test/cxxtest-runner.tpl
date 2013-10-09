<CxxTest preamble>

int main( int argc, char *argv[] )
{
    int status;
    std::ofstream ofstr("cxxtest-report-compute-pi-test.xml");
    CxxTest::XUnitPrinter tmp(ofstr);
    status = CxxTest::Main< CxxTest::XUnitPrinter >( tmp, argc, argv );
    return status;
}

// The CxxTest "world"
<CxxTest world>