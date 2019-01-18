//
//  PlayersViewController.swift
//  iosApp
//
//  Created by Kevin Galligan on 1/18/19.
//  Copyright © 2019 Kevin Galligan. All rights reserved.
//

import Foundation
import UIKit
import common

class PlayersViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    var playerData:PlayerData? = nil
    var teamId:Int64 = -1
    @IBOutlet weak var tableView: UITableView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
    
        tableView.delegate = self
        tableView.dataSource = self
        
        playerData = PlayerData(teamId: teamId, updateNotifier: showPlayers)
    }
    
    func showPlayers() -> KotlinUnit{
        tableView.reloadData()
        return KotlinUnit()
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if(playerData == nil){
        return 0
        }
        else{
        return Int(playerData!.size)
        }
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "playerRow", for: indexPath) as! PlayerRow
        playerData?.fillRow(index: Int32(indexPath.row), cell: cell)
        
        cell.layer.isOpaque = true
        
        return cell
    }
}
