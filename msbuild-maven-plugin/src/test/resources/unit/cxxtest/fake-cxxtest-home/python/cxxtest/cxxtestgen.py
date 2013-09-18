import sys

def main(args = sys.argv):
    print ",".join(args)    
    print >> sys.stderr, ",".join(args)
