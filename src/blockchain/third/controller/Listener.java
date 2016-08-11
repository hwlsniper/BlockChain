package blockchain.third.controller;

import java.util.HashMap;
import java.util.Map;

import blockchain.third.bean.BROADCASTTYPY;
import blockchain.third.bean.Block;
import blockchain.third.bean.Constants;
import blockchain.third.bean.DB;
import blockchain.third.bean.GlobalVariable;
import blockchain.third.bean.Message;
import blockchain.third.communication.BroadCast;
import blockchain.third.communication.BroadListener;

public class Listener extends BroadListener {

	public static boolean state = false;

	public Listener(int p) {
		super(p);
		// TODO Auto-generated constructor stub
	}

	public void doIT(String info) {
		System.out.println("*************" + info);

		// 监听响应请求
		if (port == GlobalVariable.requestResponsePort) {
			Message msg = new Message(info);
			if (GlobalVariable.ID.equals(msg.receiver)) {
				Message s_msg = new Message(info);

				s_msg.operation_code = Constants.RESB;
				s_msg.receiver = msg.sender;
				s_msg.sender = msg.receiver;
				s_msg.timestamp = msg.timestamp;
				// // decide send what response in each time;
				if (true) {
					s_msg.value = 1;
				} else {
					s_msg.value = 0;
				}
				System.out.println(GlobalVariable.ID + "_" + "get a  request");
				MakeConcensus.broadcast(BROADCASTTYPY.SENDRESPOSE,
						s_msg.toString());
			}
		}

		// 监听BLOCK请求
		else if (port == GlobalVariable.requestBlockPort) {
			MakeConcensus.broadcast(BROADCASTTYPY.SENDBLOCK,
					MakeConcensus.m_tmpBlock.toString());
			// send block;
			System.out
					.println(GlobalVariable.ID + "_" + "get a  block request");
		}

		// 接受响应
		else if (port == GlobalVariable.sendResponsePort) {
			Message msg = new Message(info);

			// add a new record;
			// if record size over a num, then ........;
			Message t_msg = MakeConcensus.msg_map.get(msg.timestamp);
			if (t_msg != null) {
				System.out.println(GlobalVariable.ID + "_" + "get a  response");
				MakeConcensus.m_tmpBlock.addRecord(MakeConcensus.msg_map.get(
						msg.timestamp).toString());
				MakeConcensus.msg_map.remove(msg.timestamp);

				if (MakeConcensus.m_tmpBlock.getBlockSize() >= GlobalVariable.blockMaxRecord) {
					if (GlobalVariable.isSpeaker) {
						MakeConcensus.broadcast(BROADCASTTYPY.REQUSTBLOCK, "");
					} else {
						MakeConcensus.broadcast(BROADCASTTYPY.SENDBLOCK,
								MakeConcensus.m_tmpBlock.toString());
					}

				}
			}

		}

		// 接受BLOCK
		else if (port == GlobalVariable.sendBlockPort) {
			Block block = new Block(info);
			if (GlobalVariable.isSpeaker == false) {
				// write final block
				DB.getDBInstance().addBlock(block);
				return;
			}
			
			MakeConcensus.block_arr.add(block);

			System.out.println(GlobalVariable.ID + "_" + "get a  block");
			if (GlobalVariable.isSpeaker
					&& MakeConcensus.block_arr.size() >= GlobalVariable.maxIpTable) {
				Map<String, Integer> blk_map = new HashMap<String, Integer>();

				for (Block b : MakeConcensus.block_arr) {
					if (blk_map.get(b.hash) == null || blk_map.get(b.hash) == 0)
						blk_map.put(b.hash, 1);
					else
						blk_map.put(b.hash, blk_map.get(b.hash) + 1);

				}
				int max = -100;
				String max_hash = "";
				for (String keyString : blk_map.keySet()) {
					if (blk_map.get(keyString).intValue() > max) {
						max = blk_map.get(keyString);
						max_hash = keyString;
					}
				}
				Block res_block = null;
				for (Block b : MakeConcensus.block_arr) {
					if (max_hash.equals(b.hash)) {
						res_block = b;
						break;
					}
				}

				MakeConcensus.choseNextSpeaker();
				MakeConcensus.finalBlock = res_block;
				MakeConcensus.broadcast(BROADCASTTYPY.SENDBLOCK,
						MakeConcensus.finalBlock.toString());
				DB.getDBInstance().addBlock(MakeConcensus.finalBlock);

			}
		}

		else if (port == GlobalVariable.receveSpeakerIDPort) {
			System.out.println(GlobalVariable.ID + "_" + "become a speaker");
			if (GlobalVariable.ID.equals(info)) {
				GlobalVariable.isSpeaker = true;
			}
		}

		// 用来重写的方法
		System.out.println("DoSomething" + info);
	}

}