package ccc.chess.engine.stockfish;

interface IChessEngineService 
{
	String getInfoFromEngineService(in String infoId); 
	boolean startNewProcess(in String callPid);
 	void writeLineToProcess(in String data); 
 	String readLineFromProcess(in int timeoutMillis);
}
